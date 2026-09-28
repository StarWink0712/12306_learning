package com.guoxu.payservice.mq.produce;

import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.StrUtil;
import com.guoxu.payservice.common.constant.PayRocketMQConstant;
import com.guoxu.payservice.mq.domain.MessageWrapper;
import com.guoxu.payservice.mq.event.PayResultCallbackOrderEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageConst;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

/**
 * PayResultCallbackOrderSendProduce
 * 支付结果回调订单发送消息生产者
 * @author 执笔画棠
 * @date 2025/11/11 18:25
 **/
@Slf4j
@Component
// 支付结果回调订单消息发送生产者类，继承抽象通用消息发送模板
public class PayResultCallbackOrderSendProduce extends AbstractCommonSendProduceTemplate<PayResultCallbackOrderEvent>{
    // 可配置环境对象，用于读取配置文件中的属性
    private final ConfigurableEnvironment environment;

    // 构造函数，通过@Autowired自动注入依赖
    public PayResultCallbackOrderSendProduce(@Autowired RocketMQTemplate rocketMQTemplate,
                                             @Autowired ConfigurableEnvironment environment) {
        // 调用父类构造函数，传入RocketMQ模板
        super(rocketMQTemplate);
        // 初始化环境配置对象
        this.environment = environment;
    }

    // 重写父类方法，构建消息发送的基本扩展参数
    @Override
    protected BaseSendExtendDTO buildBaseSendExtendParam(PayResultCallbackOrderEvent messageSendEvent) {
        // 使用建造者模式构建基础发送扩展参数DTO
        return BaseSendExtendDTO.builder()
                // 设置事件名称，用于日志记录和监控
                .eventName("支付结果回调订单")
                // 设置消息键，使用订单号作为唯一标识，用于消息追踪
                .keys(messageSendEvent.getOrderSn())
                // 设置消息主题，从配置文件中解析主题键值
                .topic(environment.resolvePlaceholders(PayRocketMQConstant.PAY_GLOBAL_TOPIC_KEY))
                // 设置消息标签，从配置文件中解析标签键值，用于消息过滤
                .tag(environment.resolvePlaceholders(PayRocketMQConstant.PAY_RESULT_CALLBACK_TAG_KEY))
                // 设置发送超时时间，单位毫秒
                .sentTimeout(2000L)
                // 完成构建
                .build();
    }



    // 重写父类方法，构建具体的消息对象
    @Override
    protected Message<?> buildMessage(PayResultCallbackOrderEvent messageSendEvent, BaseSendExtendDTO requestParam) {
        // 判断键值是否为空，如果为空则生成UUID作为备用键值
        String keys = StrUtil.isEmpty(requestParam.getKeys()) ? UUID.randomUUID().toString() : requestParam.getKeys();
        // 使用MessageBuilder构建Spring Message对象
        return MessageBuilder
                // 设置消息 payload，使用MessageWrapper包装业务数据和键值
                .withPayload(new MessageWrapper(requestParam.getKeys(), messageSendEvent))
                // 设置消息头中的键值属性，RocketMQ会使用此属性作为消息索引
                .setHeader(MessageConst.PROPERTY_KEYS, keys)
                // 设置消息头中的标签属性，用于消费者过滤消息
                .setHeader(MessageConst.PROPERTY_TAGS, requestParam.getTag())
                // 构建最终的Message对象
                .build();
    }
}
