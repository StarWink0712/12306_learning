package com.guoxu.orderservice.mq.consumer;

import com.guoxu.annotation.Idempotent;
import com.guoxu.enums.IdempotentSceneEnum;
import com.guoxu.enums.IdempotentTypeEnum;
import com.guoxu.orderservice.common.constant.OrderRocketMQConstant;
import com.guoxu.orderservice.common.enums.OrderItemStatusEnum;
import com.guoxu.orderservice.common.enums.OrderStatusEnum;
import com.guoxu.orderservice.dto.domain.OrderStatusReversalDTO;
import com.guoxu.orderservice.mq.domain.MessageWrapper;
import com.guoxu.orderservice.mq.event.PayResultCallbackOrderEvent;
import com.guoxu.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * PayResultCallbackOrderConsumer
 * 接收到订单支付成功后，处理一下，改变数据库中订单的状态为已支付
 * 最后，调用订单服务的支付回调订单处理方法，处理支付成功后的其他业务逻辑，如更新库存、发送通知等
 * @author 执笔画棠
 * @date 2025/11/10 17:31
 **/
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        // 指定消费的主题，从常量类获取主题键
        topic = OrderRocketMQConstant.PAY_GLOBAL_TOPIC_KEY,
        // 指定消费的标签（消息过滤表达式），只处理支付结果回调相关的消息
        selectorExpression = OrderRocketMQConstant.PAY_RESULT_CALLBACK_TAG_KEY,
        // 指定消费者组，用于集群消费和负载均衡
        consumerGroup = OrderRocketMQConstant.PAY_RESULT_CALLBACK_ORDER_CG_KEY)
public class PayResultCallbackOrderConsumer implements RocketMQListener<MessageWrapper<PayResultCallbackOrderEvent>> {

    //订单服务，用于处理订单状态更新等业务逻辑
    private final OrderService orderService;

    /*
     * 幂等性注解
     * 确保每个支付结果回调消息只被处理一次，避免重复处理
     */
    @Idempotent(
            // 唯一键前缀，用于Redis键的命名空间隔离
            uniqueKeyPrefix = "index12306-order:pay_result_callback:",
            // 幂等键的SPEL表达式，使用消息的keys和hashCode组合确保唯一性
            key = "#message.getKeys()+'_'+#message.hashCode()",
            // 幂等类型为SPEL表达式
            type = IdempotentTypeEnum.SPEL,
            // 使用场景为消息队列
            scene = IdempotentSceneEnum.MQ,
            // 键的过期时间，设置为7200秒（2小时）
            keyTimeout = 7200L)
    @Transactional(rollbackFor = Exception.class)
    /*
     * RocketMQ消息监听方法
     * 当监听到匹配的消息时会自动调用此方法
     *
     * @param message 消息包装对象，包含支付结果回调订单事件
     */
    @Override
    public void onMessage(MessageWrapper<PayResultCallbackOrderEvent> message) {
        //从消息包装器中获取事件的支付结果回调订单事件对象，即实际的消息体
        PayResultCallbackOrderEvent payResultCallbackOrderEvent=message.getMessage();

        //构建订单状态反转数据传输对象,即改变订单主表和订单项的状态为已支付
        //用于更新订单主表和订单项的状态为已支付
        OrderStatusReversalDTO orderStatusReversalDTO=OrderStatusReversalDTO.builder()
                //设置订单号，从支付结果事件中获取
                .orderSn(payResultCallbackOrderEvent.getOrderSn())
                // 设置订单状态为"已支付"
                .orderStatus(OrderStatusEnum.ALREADY_PAID.getStatus())
                // 设置订单项状态为"已支付"
                .orderItemStatus(OrderItemStatusEnum.ALREADY_PAID.getStatus())
                // 构建完整的DTO对象
                .build();

                // 调用订单服务更新订单状态
        orderService.statusReversal(orderStatusReversalDTO);

        // 调用订单服务的支付回调订单处理方法
        // 处理支付成功后的其他业务逻辑，如更新库存、发送通知等
        orderService.payCallbackOrder(payResultCallbackOrderEvent);
    }
}
