package com.guoxu.orderservice.mq.consumer;

import com.guoxu.annotation.Idempotent;
import com.guoxu.enums.IdempotentSceneEnum;
import com.guoxu.enums.IdempotentTypeEnum;
import com.guoxu.orderservice.common.constant.OrderRocketMQConstant;
import com.guoxu.orderservice.common.enums.OrderItemStatusEnum;
import com.guoxu.orderservice.common.enums.OrderStatusEnum;
import com.guoxu.orderservice.dao.entity.OrderItemDO;
import com.guoxu.orderservice.dto.domain.OrderItemStatusReversalDTO;
import com.guoxu.orderservice.dto.resp.TicketOrderPassengerDetailRespDTO;
import com.guoxu.orderservice.mq.domain.MessageWrapper;
import com.guoxu.orderservice.mq.event.RefundResultCallbackOrderEvent;
import com.guoxu.orderservice.service.OrderItemService;
import com.guoxu.toolkit.BeanUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * RefundResultCallbackOrderConsumer
 *
 * @author 执笔画棠
 * @date 2025/11/10 18:32
 **/
@Slf4j
@Component
@RequiredArgsConstructor
//这个注解的作用是监听指定的RocketMQ主题和标签，当有消息到达时，会调用对应的方法进行处理
@RocketMQMessageListener(topic = OrderRocketMQConstant.PAY_GLOBAL_TOPIC_KEY,
        selectorExpression = OrderRocketMQConstant.REFUND_RESULT_CALLBACK_TAG_KEY,
        consumerGroup = OrderRocketMQConstant.REFUND_RESULT_CALLBACK_ORDER_CG_KEY)
/**
 * 退款结果回调订单消费者
 * 用于监听退款结果消息，并根据退款类型更新订单状态
 * 实现RocketMQListener接口，处理退款回调事件
 */
public class RefundResultCallbackOrderConsumer {
    // 订单项服务，用于处理订单项状态更新等业务逻辑
    private final OrderItemService orderItemService;

    /**
     * 幂等性注解，防止消息重复消费
     * 在分布式环境下确保同一退款消息不会被重复处理
     */
    @Idempotent(
            // 唯一键前缀，用于Redis键的命名空间隔离，与退款相关
            uniqueKeyPrefix = "index12306-order:refund_result_callback:",
            // 幂等键的SPEL表达式，使用消息的keys和hashCode组合确保唯一性
            key = "#message.getKeys()+'_'+#message.hashCode()",
            // 幂等类型为SPEL表达式
            type = IdempotentTypeEnum.SPEL,
            // 使用场景为消息队列
            scene = IdempotentSceneEnum.MQ,
            // 键的过期时间，设置为7200秒（2小时）
            keyTimeout = 7200L)
    /**
     * 事务注解，确保方法执行在事务中
     * 出现任何异常时都会回滚事务，保证数据一致性
     */
    @Transactional(rollbackFor = Exception.class)
    /**
     * RocketMQ消息监听方法
     * 当监听到退款结果消息时会自动调用此方法
     *
     * @param message 消息包装对象，包含退款结果回调订单事件
     */
    @Override
    public void onMessage(MessageWrapper<RefundResultCallbackOrderEvent> message) {
        // 从消息包装器中获取实际的退款结果回调订单事件对象
        RefundResultCallbackOrderEvent refundResultCallbackOrderEvent = message.getMessage();

        // 获取退款类型对应的状态码，用于判断是否为部分退款
        Integer status = refundResultCallbackOrderEvent.getRefundTypeEnum().getCode();

        // 获取订单号
        String orderSn = refundResultCallbackOrderEvent.getOrderSn();

        // 创建订单项数据对象列表，用于存储需要更新的订单项
        List<OrderItemDO> orderItemDOList = new ArrayList<>();

        // 获取部分退款票详情列表
        List<TicketOrderPassengerDetailRespDTO> partialRefundTicketDetailList = refundResultCallbackOrderEvent
                .getPartialRefundTicketDetailList();

        // 遍历部分退款票详情列表，将DTO转换为DO对象
        partialRefundTicketDetailList.forEach(partial -> {
            // 创建订单项数据对象
            OrderItemDO orderItemDO = new OrderItemDO();

            // 使用BeanUtil工具类将部分退款票详情对象的属性拷贝到订单项数据对象中
            BeanUtil.convert(partial, orderItemDO);

            // 将转换后的订单项数据对象添加到列表中
            orderItemDOList.add(orderItemDO);
        });

        // 判断退款类型是否为部分退款
        if (status.equals(OrderStatusEnum.PARTIAL_REFUND.getStatus())) {
            // 构建部分退款订单项状态反转数据传输对象
            OrderItemStatusReversalDTO partialRefundOrderItemStatusReversalDTO = OrderItemStatusReversalDTO.builder()
                    // 设置订单号
                    .orderSn(orderSn)
                    // 设置订单状态为"部分退款"
                    .orderStatus(OrderStatusEnum.PARTIAL_REFUND.getStatus())
                    // 设置订单项状态为"已退款"
                    .orderItemStatus(OrderItemStatusEnum.REFUNDED.getStatus())
                    // 设置需要更新的订单项列表
                    .orderItemDOList(orderItemDOList)
                    // 构建完整的DTO对象
                    .build();

            // 调用订单项服务的状态反转方法，更新订单项状态
            orderItemService.orderItemStatusReversal(partialRefundOrderItemStatusReversalDTO);
        }
        // 判断退款类型是否为全额退款
        else if (status.equals(OrderStatusEnum.FULL_REFUND.getStatus())) {
            // 构建全额退款订单项状态反转数据传输对象
            OrderItemStatusReversalDTO fullRefundOrderItemStatusReversalDTO = OrderItemStatusReversalDTO.builder()
                    // 设置订单号
                    .orderSn(orderSn)
                    // 设置订单状态为"全额退款"
                    .orderStatus(OrderStatusEnum.FULL_REFUND.getStatus())
                    // 设置订单项状态为"已退款"
                    .orderItemStatus(OrderItemStatusEnum.REFUNDED.getStatus())
                    // 设置需要更新的订单项列表
                    .orderItemDOList(orderItemDOList)
                    // 构建完整的DTO对象
                    .build();

            // 调用订单项服务的状态反转方法，更新订单项状态
            orderItemService.orderItemStatusReversal(fullRefundOrderItemStatusReversalDTO);
        }
    }
}
