package com.guoxu.orderservice.mq.produce;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.Message;
import org.apache.rocketmq.client.producer.SendResult;

import java.util.Optional;

/**
 * AbstractCommonSendProduceTemplate
 * RocketMQ 公共发送模板
 * 提供了通用的发送方法，用于发送订单服务事件到指定的 RocketMQ 主题。
 * 这个抽象类 AbstractCommonSendProduceTemplate 定义了一个标准的、不可变的消息发送骨架，
 * 同时将易变的部分（如何构建消息参数、如何构建消息体）延迟到子类中去实现。
 * @author 执笔画棠
 * @date 2025/11/10 19:43
 **/
@Slf4j
@RequiredArgsConstructor
public abstract class AbstractCommonSendProduceTemplate<T> {
    //rocketmq模版，用于实际的消息发送操作
    private final RocketMQTemplate rocketMQTemplate;

    /**
     * 构建消息发送事件基础扩充属性实体
     * 抽象方法，子类必须实现，用于构建消息发送的基础参数
     *
     * @param messageSendEvent 消息发送事件
     * @return 扩充属性实体，包含主题、标签、超时时间等基础信息
     */
    protected abstract BaseSendExtendDTO buildBaseSendExtendParam(T messageSendEvent);


    /**
     * 构建消息基本参数，包括请求头、Keys等
     * 抽象方法，子类必须实现，用于构建具体的消息对象
     *
     * @param messageSendEvent 消息发送事件
     * @param requestParam     扩充属性实体
     * @return 构建完成的消息对象
     */
    protected abstract Message<?> buildMessage(T messageSendEvent, BaseSendExtendDTO requestParam);

    /**
     * 消息事件通用发送方法
     * 模板方法，定义了消息发送的标准流程
     *
     * @param messageSendEvent 消息发送事件
     * @return 消息发送返回结果
     */
    public SendResult sendMessage(T messageSendEvent) {
        // 调用抽象方法构建基础发送扩展参数
        BaseSendExtendDTO baseSendExtendDTO = buildBaseSendExtendParam(messageSendEvent);

        // 定义发送结果变量
        SendResult sendResult;

        // 使用try-catch块捕获发送过程中的异常
        try {
            // 使用StringBuilder构建目标地址（主题:标签）
            StringBuilder destinationBuilder = StrUtil.builder().append(baseSendExtendDTO.getTopic());

            // 如果标签不为空，则添加到目标地址中
            if (StrUtil.isNotBlank(baseSendExtendDTO.getTag())) {
                destinationBuilder.append(":").append(baseSendExtendDTO.getTag());
            }

            // 同步发送消息到RocketMQ
            sendResult = rocketMQTemplate.syncSend(
                    // 目标地址，格式为"主题"或"主题:标签"
                    destinationBuilder.toString(),
                    // 调用抽象方法构建的消息对象
                    buildMessage(messageSendEvent, baseSendExtendDTO),
                    // 发送超时时间
                    baseSendExtendDTO.getSentTimeout(),
                    // 延迟级别，如果为空则使用默认值0（不延迟）
                    Optional.ofNullable(baseSendExtendDTO.getDelayLevel()).orElse(0));

            // 记录消息发送成功的日志，包括事件名称、发送状态、消息ID和消息Keys
            log.info("[{}] 消息发送结果：{}，消息ID：{}，消息Keys：{}", baseSendExtendDTO.getEventName(), sendResult.getSendStatus(),
                    sendResult.getMsgId(), baseSendExtendDTO.getKeys());
        } catch (Throwable ex) {
            // 记录消息发送失败的日志，包括事件名称、消息体和异常信息
            log.error("[{}] 消息发送失败，消息体：{}", baseSendExtendDTO.getEventName(), JSON.toJSONString(messageSendEvent), ex);

            // 重新抛出异常，让调用方处理
            throw ex;
        }

        // 返回发送结果
        return sendResult;
    }

}
