package com.guoxu.orderservice.mq.produce;

import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.StrUtil;
import com.guoxu.orderservice.common.constant.OrderRocketMQConstant;
import com.guoxu.orderservice.mq.domain.MessageWrapper;
import com.guoxu.orderservice.mq.event.DelayCloseOrderEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageConst;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

/**
 * DelayCloseOrderSendProduce
 * 延迟关闭订单生成者
 * 延迟关闭订单消息生产者
 * 继承自抽象通用消息发送模板，专门用于发送延迟关闭订单的消息
 * 使用RocketMQ的延迟消息功能实现订单超时自动关闭
 * @author 执笔画棠
 * @date 2025/11/10 19:56
 **/
@Slf4j
@Component
public class DelayCloseOrderSendProduce extends AbstractCommonSendProduceTemplate<DelayCloseOrderEvent> {

    // 可配置的环境对象，用于解析配置文件中的占位符
    private final ConfigurableEnvironment environment;

    /**
     * 构造函数，通过依赖注入初始化RocketMQ模板和环境配置
     *
     * @param rocketMQTemplate RocketMQ消息模板，由Spring自动注入
     * @param environment      环境配置对象，由Spring自动注入，用于解析配置占位符
     */
    public DelayCloseOrderSendProduce(@Autowired RocketMQTemplate rocketMQTemplate,
                                      @Autowired ConfigurableEnvironment environment) {
        // 调用父类构造函数，传入RocketMQ模板
        super(rocketMQTemplate);
        // 初始化环境配置对象
        this.environment = environment;
    }

    /**
     * 构建消息发送的基础扩展参数
     * 实现父类的抽象方法，定义延迟关闭订单消息的发送参数
     *
     * @param messageSendEvent 延迟关闭订单事件对象
     * @return 构建完成的基础发送扩展DTO对象
     */
    @Override
    protected BaseSendExtendDTO buildBaseSendExtendParam(DelayCloseOrderEvent messageSendEvent) {
        // 使用建造者模式构建基础发送扩展参数
        return BaseSendExtendDTO.builder()
                // 设置事件名称为"延迟关闭订单"，用于日志记录和监控
                .eventName("延迟关闭订单")
                // 设置消息的Keys为订单号，用于消息追踪和去重
                .keys(messageSendEvent.getOrderSn())
                // 从配置文件中解析主题键，支持动态配置
                .topic(environment.resolvePlaceholders(OrderRocketMQConstant.ORDER_DELAY_CLOSE_TOPIC_KEY))
                // 从配置文件中解析标签键，支持动态配置
                .tag(environment.resolvePlaceholders(OrderRocketMQConstant.ORDER_DELAY_CLOSE_TAG_KEY))
                // 设置发送超时时间为2000毫秒（2秒）
                .sentTimeout(2000L)
                // 设置延迟级别为14，对应10分钟延迟
                // RocketMQ 延迟消息级别说明：
                // 1s 5s 10s 30s 1m 2m 3m 4m 5m 6m 7m 8m 9m 10m 20m 30m 1h 2h
                // 级别1=1秒，2=5秒，3=10秒，4=30秒，5=1分钟，6=2分钟，7=3分钟，8=4分钟，
                // 9=5分钟，10=6分钟，11=7分钟，12=8分钟，13=9分钟，14=10分钟，15=20分钟，
                // 16=30分钟，17=1小时，18=2小时
                .delayLevel(14)
                // 构建完整的DTO对象
                .build();
    }

    /**
     * 构建具体的消息对象
     * 实现父类的抽象方法，包装延迟关闭订单事件为RocketMQ消息
     *
     * @param messageSendEvent 延迟关闭订单事件对象
     * @param requestParam     基础发送扩展参数
     * @return 构建完成的RocketMQ消息对象
     */
    @Override
    protected Message<?> buildMessage(DelayCloseOrderEvent messageSendEvent, BaseSendExtendDTO requestParam) {
        // 如果请求参数中的Keys为空，则生成一个随机UUID作为备用Keys
        // 否则使用请求参数中的Keys（订单号）
        String keys = StrUtil.isEmpty(requestParam.getKeys()) ? UUID.randomUUID().toString() : requestParam.getKeys();

        // 使用Spring的MessageBuilder构建消息对象
        return MessageBuilder
                // 设置消息负载，使用消息包装器包装订单号和事件对象
                .withPayload(new MessageWrapper(requestParam.getKeys(), messageSendEvent))
                // 设置消息头中的Keys属性，用于消息索引和查询
                .setHeader(MessageConst.PROPERTY_KEYS, keys)
                // 设置消息头中的Tags属性，用于消息过滤
                .setHeader(MessageConst.PROPERTY_TAGS, requestParam.getTag())
                // 构建完整的消息对象
                .build();
    }
}
