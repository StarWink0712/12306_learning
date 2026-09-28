package com.guoxu.ticketservice.mq.consumer;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.guoxu.annotation.Idempotent;
import com.guoxu.enums.IdempotentSceneEnum;
import com.guoxu.enums.IdempotentTypeEnum;
import com.guoxu.exception.ServiceException;
import com.guoxu.result.Result;
import com.guoxu.ticketservice.common.constant.TicketRocketMQConstant;
import com.guoxu.ticketservice.common.enums.SeatStatusEnum;
import com.guoxu.ticketservice.dao.entity.SeatDO;
import com.guoxu.ticketservice.dao.mapper.SeatMapper;
import com.guoxu.ticketservice.dto.resp.TicketOrderDetailRespDTO;
import com.guoxu.ticketservice.mq.domain.MessageWrapper;
import com.guoxu.ticketservice.mq.event.PayResultCallbackTicketEvent;
import com.guoxu.ticketservice.remote.TicketOrderRemoteService;
import com.guoxu.ticketservice.remote.dto.TicketOrderPassengerDetailRespDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * PayResultCallbackTicketConsumer
 *
 * @author 执笔画棠
 * @date 2025/11/12 22:09
 **/
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(topic = TicketRocketMQConstant.PAY_GLOBAL_TOPIC_KEY, selectorExpression = TicketRocketMQConstant.PAY_RESULT_CALLBACK_TAG_KEY, consumerGroup = TicketRocketMQConstant.PAY_RESULT_CALLBACK_TICKET_CG_KEY)
// PayResultCallbackTicketConsumer类实现了RocketMQListener接口，
// 用于处理支付结果回调的车票相关消息，主要功能是根据支付结果更新座位状态
public class PayResultCallbackTicketConsumer implements RocketMQListener<MessageWrapper<PayResultCallbackTicketEvent>> {

    // 注入TicketOrderRemoteService，用于远程查询车票订单详细信息
    private final TicketOrderRemoteService ticketOrderRemoteService;
    // 注入SeatMapper，用于操作座位相关的数据库表
    private final SeatMapper seatMapper;

    // 幂等性注解，确保MQ消息消费的幂等性
    // uniqueKeyPrefix指定唯一键前缀为"index12306 - ticket:pay_result_callback:"
    // key通过SpEL表达式生成，结合消息的键和哈希码
    // type指定幂等性类型为SPEL表达式方式
    // scene指定幂等性场景为MQ
    // keyTimeout设置唯一键的过期时间为7200秒
    @Idempotent(uniqueKeyPrefix = "index12306 - ticket:pay_result_callback:", key = "#message.getKeys()+'_'+#message.hashCode()", type = IdempotentTypeEnum.SPEL, scene = IdempotentSceneEnum.MQ, keyTimeout = 7200L)
    // 事务注解，声明该方法内的数据库操作在一个事务中，遇到异常时回滚事务
    @Transactional(rollbackFor = Exception.class)
    // 重写RocketMQListener接口的onMessage方法，处理接收到的消息
    @Override
    public void onMessage(MessageWrapper<PayResultCallbackTicketEvent> message) {
        Result<TicketOrderDetailRespDTO> ticketOrderDetailResult;
        try {
            // 通过远程服务根据订单号查询车票订单详细信息
            ticketOrderDetailResult = ticketOrderRemoteService
                    .queryTicketOrderByOrderSn(message.getMessage().getOrderSn());
            // 如果查询不成功且返回的数据为空
            if (!ticketOrderDetailResult.isSuccess() && Objects.isNull(ticketOrderDetailResult.getData())) {
                // 抛出业务异常，提示支付结果回调查询订单失败
                throw new ServiceException("支付结果回调查询订单失败");
            }
        } catch (Throwable ex) {
            // 记录错误日志，表明支付结果回调查询订单失败，并记录异常信息
            log.error("支付结果回调查询订单失败", ex);
            // 重新抛出捕获的异常
            throw ex;
        }
        // 获取查询到的车票订单详细信息
        TicketOrderDetailRespDTO ticketOrderDetail = ticketOrderDetailResult.getData();
        // 遍历车票订单中的乘客详细信息
        for (TicketOrderPassengerDetailRespDTO each : ticketOrderDetail.getPassengerDetails()) {
            // 创建LambdaUpdateWrapper对象，用于构建更新座位状态的条件
            LambdaUpdateWrapper<SeatDO> updateWrapper = Wrappers.lambdaUpdate(SeatDO.class)
                    .eq(SeatDO::getTrainId, ticketOrderDetail.getTrainId())
                    .eq(SeatDO::getCarriageNumber, each.getCarriageNumber())
                    .eq(SeatDO::getSeatNumber, each.getSeatNumber())
                    .eq(SeatDO::getSeatType, each.getSeatType())
                    .eq(SeatDO::getStartStation, ticketOrderDetail.getDeparture())
                    .eq(SeatDO::getEndStation, ticketOrderDetail.getArrival());
            // 创建SeatDO对象，设置要更新的座位状态为已售出
            SeatDO updateSeatDO = new SeatDO();
            updateSeatDO.setSeatStatus(SeatStatusEnum.SOLD.getCode());
            // 使用seatMapper根据构建的更新条件和更新数据，更新数据库中的座位状态
            seatMapper.update(updateSeatDO, updateWrapper);
        }
    }
}