package com.guoxu.ticketservice.mq.consumer;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.guoxu.DistributedCache;
import com.guoxu.annotation.Idempotent;
import com.guoxu.enums.IdempotentSceneEnum;
import com.guoxu.enums.IdempotentTypeEnum;
import com.guoxu.result.Result;
import com.guoxu.ticketservice.common.constant.TicketRocketMQConstant;
import com.guoxu.ticketservice.dto.domain.RouteDTO;
import com.guoxu.ticketservice.dto.req.CancelTicketOrderReqDTO;
import com.guoxu.ticketservice.mq.domain.MessageWrapper;
import com.guoxu.ticketservice.mq.event.DelayCloseOrderEvent;
import com.guoxu.ticketservice.remote.TicketOrderRemoteService;
import com.guoxu.ticketservice.remote.dto.TicketOrderDetailRespDTO;
import com.guoxu.ticketservice.remote.dto.TicketOrderPassengerDetailRespDTO;
import com.guoxu.ticketservice.service.SeatService;
import com.guoxu.ticketservice.service.TrainStationService;
import com.guoxu.ticketservice.service.handler.ticket.dto.TrainPurchaseTicketRespDTO;
import com.guoxu.ticketservice.service.handler.ticket.tokenbucket.TicketAvailabilityTokenBucket;
import com.guoxu.toolkit.BeanUtil;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.guoxu.ticketservice.common.constant.RedisKeyConstant.TRAIN_STATION_REMAINING_TICKET;

/**
 * DelayCloseOrderConsumer
 *
 * @author 执笔画棠
 * @date 2025/11/12 22:09
 **/
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(topic = TicketRocketMQConstant.ORDER_DELAY_CLOSE_TOPIC_KEY, selectorExpression = TicketRocketMQConstant.ORDER_DELAY_CLOSE_TAG_KEY, consumerGroup = TicketRocketMQConstant.TICKET_DELAY_CLOSE_CG_KEY)
// 延迟关闭订单消息消费者类，实现RocketMQ消息监听接口
public class DelayCloseOrderConsumer implements RocketMQListener<MessageWrapper<DelayCloseOrderEvent>> {

    // 座位服务，用于处理座位相关的业务逻辑
    private final SeatService seatService;
    // 订单远程服务，用于调用订单服务的远程接口
    private final TicketOrderRemoteService ticketOrderRemoteService;
    // 列车站点服务，用于处理列车站点相关的业务逻辑
    private final TrainStationService trainStationService;
    // 分布式缓存，用于操作Redis等缓存系统
    private final DistributedCache distributedCache;
    // 票务可用性令牌桶，用于管理票务库存的令牌
    private final TicketAvailabilityTokenBucket ticketAvailabilityTokenBucket;

    // 从配置文件中注入票务缓存更新类型的配置值
    @Value("${ticket.availability.cache-update.type:}")
    private String ticketAvailabilityCacheUpdateType;

    // 幂等性注解，防止消息重复消费
    @Idempotent(uniqueKeyPrefix = "index12306-ticket:delay_close_order:", // 唯一键前缀
            key = "#delayCloseOrderEventMessageWrapper.getKeys()+'_'+#delayCloseOrderEventMessageWrapper.hashCode()", // 使用消息的keys和hashCode构建唯一键
            type = IdempotentTypeEnum.SPEL, // 使用SpEL表达式类型
            scene = IdempotentSceneEnum.MQ, // 应用场景为消息队列
            keyTimeout = 7200L // 键的超时时间为7200秒（2小时）
    )
    // 重写消息处理方法，处理接收到的RocketMQ消息
    @Override
    public void onMessage(MessageWrapper<DelayCloseOrderEvent> delayCloseOrderEventMessageWrapper) {
        // 记录日志：开始消费延迟关闭订单消息
        log.info("[延迟关闭订单] 开始消费：{}", JSON.toJSONString(delayCloseOrderEventMessageWrapper));
        // 从消息包装器中获取实际的延迟关闭订单事件
        DelayCloseOrderEvent delayCloseOrderEvent = delayCloseOrderEventMessageWrapper.getMessage();
        // 从事件中获取订单号
        String orderSn = delayCloseOrderEvent.getOrderSn();
        // 定义关闭订单的结果对象
        Result<Boolean> closedTickOrder;
        try {
            // 调用远程订单服务关闭订单，传入取消订单请求DTO
            closedTickOrder = ticketOrderRemoteService.closeTickOrder(new CancelTicketOrderReqDTO(orderSn));
        } catch (Throwable ex) {
            // 记录错误日志：远程调用订单服务失败
            log.error("[延迟关闭订单] 订单号：{} 远程调用订单服务失败", orderSn, ex);
            // 重新抛出异常
            throw ex;
        }
        // 判断：如果远程调用成功且缓存更新类型不是binlog模式
        if (closedTickOrder.isSuccess() && !StrUtil.equals(ticketAvailabilityCacheUpdateType, "binlog")) {
            // 判断：如果关闭订单返回的数据为false，表示用户已支付订单
            if (!closedTickOrder.getData()) {
                // 记录日志：用户已支付订单，无需后续处理
                log.info("[延迟关闭订单] 订单号：{} 用户已支付订单", orderSn);
                // 直接返回，结束方法执行
                return;
            }
            // 从事件中获取列车ID
            String trainId = delayCloseOrderEvent.getTrainId();
            // 从事件中获取出发站
            String departure = delayCloseOrderEvent.getDeparture();
            // 从事件中获取到达站
            String arrival = delayCloseOrderEvent.getArrival();
            // 从事件中获取购票结果列表
            List<TrainPurchaseTicketRespDTO> trainPurchaseTicketResults = delayCloseOrderEvent
                    .getTrainPurchaseTicketResults();
            try {
                // 调用座位服务解锁座位，释放被占用的座位
                seatService.unlock(trainId, departure, arrival, trainPurchaseTicketResults);
            } catch (Throwable ex) {
                // 记录错误日志：回滚列车数据库座位状态失败
                log.error("[延迟关闭订单] 订单号：{} 回滚列车DB座位状态失败", orderSn, ex);
                // 重新抛出异常
                throw ex;
            }
            try {
                // 获取分布式缓存的StringRedisTemplate实例
                StringRedisTemplate stringRedisTemplate = (StringRedisTemplate) distributedCache.getInstance();
                // 将购票结果按座位类型分组，生成座位类型映射
                Map<Integer, List<TrainPurchaseTicketRespDTO>> seatTypeMap = trainPurchaseTicketResults.stream()
                        .collect(Collectors.groupingBy(TrainPurchaseTicketRespDTO::getSeatType));
                // 查询列车经过的站点路线列表
                List<RouteDTO> routeDTOList = trainStationService.listTakeoutTrainStationRoute(trainId, departure,
                        arrival);
                // 遍历每个路线段
                routeDTOList.forEach(each -> {
                    // 构建缓存键后缀：列车ID_起始站_终点站
                    String keySuffix = StrUtil.join("_", trainId, each.getStartStation(), each.getEndStation());
                    // 遍历每个座位类型的分组
                    seatTypeMap.forEach((seatType, trainPurchaseTicketRespDTOList) -> {
                        // 在Redis哈希中增加对应座位类型的余票数量
                        stringRedisTemplate.opsForHash()
                                .increment(TRAIN_STATION_REMAINING_TICKET + keySuffix, String.valueOf(seatType),
                                        trainPurchaseTicketRespDTOList.size()); // 增加的数量为该座位类型的购票记录数量
                    });
                });
                // 将延迟关闭订单事件转换为订单详情响应DTO
                TicketOrderDetailRespDTO ticketOrderDetail = BeanUtil.convert(delayCloseOrderEvent,
                        TicketOrderDetailRespDTO.class);
                // 设置乘客详情信息
                ticketOrderDetail.setPassengerDetails(BeanUtil.convert(
                        delayCloseOrderEvent.getTrainPurchaseTicketResults(), TicketOrderPassengerDetailRespDTO.class));
                // 在令牌桶中回滚库存
                ticketAvailabilityTokenBucket.rollbackInBucket(ticketOrderDetail);
            } catch (Throwable ex) {
                // 记录错误日志：回滚列车缓存余票失败
                log.error("[延迟关闭订单] 订单号：{} 回滚列车Cache余票失败", orderSn, ex);
                // 重新抛出异常
                throw ex;
            }
        }
    }
}