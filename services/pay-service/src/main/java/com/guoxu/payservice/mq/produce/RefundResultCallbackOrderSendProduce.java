package com.guoxu.payservice.mq.produce;

import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.StrUtil;
import com.guoxu.payservice.mq.domain.MessageWrapper;
import com.guoxu.payservice.mq.event.RefundResultCallbackOrderEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageConst;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import static com.guoxu.payservice.common.constant.PayRocketMQConstant.PAY_GLOBAL_TOPIC_KEY;
import static com.guoxu.payservice.common.constant.PayRocketMQConstant.REFUND_RESULT_CALLBACK_TAG_KEY;

/**
 * RefundResultCallbackOrderSendProduce
 * 退款结果回调订单生产者
 * @author 执笔画棠
 * @date 2025/11/11 18:28
 **/
// RefundResultCallbackOrderSendProduce类继承自AbstractCommonSendProduceTemplate，
// 专门用于发送退款结果回调订单的事件消息到RocketMQ
    @Component
    @Slf4j
public class RefundResultCallbackOrderSendProduce  extends AbstractCommonSendProduceTemplate<RefundResultCallbackOrderEvent>{
    // 注入ConfigurableEnvironment，用于获取配置文件中的属性值
    private final ConfigurableEnvironment environment;

    // 构造函数，通过依赖注入获取RocketMQTemplate和ConfigurableEnvironment
    public RefundResultCallbackOrderSendProduce(@Autowired RocketMQTemplate rocketMQTemplate,
                                                @Autowired ConfigurableEnvironment environment) {
        // 调用父类构造函数，传入RocketMQTemplate
        super(rocketMQTemplate);
        // 初始化environment
        this.environment = environment;
    }

    // 重写父类方法，构建基本的发送扩展参数
    @Override
    protected BaseSendExtendDTO buildBaseSendExtendParam(RefundResultCallbackOrderEvent messageSendEvent) {
        // 使用Builder模式构建BaseSendExtendDTO对象
        return BaseSendExtendDTO.builder()
                // 设置事件名称为“全部退款或部分退款结果回调订单”
                .eventName("全部退款或部分退款结果回调订单")
                // 设置消息的键为退款结果回调订单事件中的订单号
                .keys(messageSendEvent.getOrderSn())
                // 从配置文件中解析并设置主题
                .topic(environment.resolvePlaceholders(PAY_GLOBAL_TOPIC_KEY))
                // 从配置文件中解析并设置标签
                .tag(environment.resolvePlaceholders(REFUND_RESULT_CALLBACK_TAG_KEY))
                // 设置发送超时时间为2000毫秒
                .sentTimeout(2000L)
                // 构建BaseSendExtendDTO对象
                .build();
    }

    // 重写父类方法，构建要发送的消息
    @Override
    protected Message<?> buildMessage(RefundResultCallbackOrderEvent messageSendEvent, BaseSendExtendDTO requestParam) {
        // 如果请求参数中的键为空，则生成一个随机的UUID作为键
        String keys = StrUtil.isEmpty(requestParam.getKeys()) ? UUID.randomUUID().toString() : requestParam.getKeys();
        // 使用MessageBuilder构建消息
        return MessageBuilder
                // 设置消息负载为包含键和退款结果回调订单事件的MessageWrapper对象
                .withPayload(new MessageWrapper(requestParam.getKeys(), messageSendEvent))
                // 设置消息头中的键属性
                .setHeader(MessageConst.PROPERTY_KEYS, keys)
                // 设置消息头中的标签属性
                .setHeader(MessageConst.PROPERTY_TAGS, requestParam.getTag())
                // 构建消息
                .build();
    }
}
