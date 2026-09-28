package com.guoxu.payservice.mq.produce;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.Message;

/**
 * AbstractCommonSendProduceTemplate
 * RocketMQ 抽象公共发送消息组件
 * @author 执笔画棠
 * @date 2025/11/11 18:22
 **/
@Slf4j
@RequiredArgsConstructor
public abstract class AbstractCommonSendProduceTemplate<T> {
    private final RocketMQTemplate rocketMQTemplate;

    /**
     * 构建消息发送事件基础扩充属性实体
     *
     * @param messageSendEvent 消息发送事件
     * @return 扩充属性实体
     */
    protected abstract BaseSendExtendDTO buildBaseSendExtendParam(T messageSendEvent);

    /**
     * 构建消息基本参数，请求头、Keys...
     *
     * @param messageSendEvent 消息发送事件
     * @param requestParam     扩充属性实体
     * @return 消息基本参数
     */
    protected abstract Message<?> buildMessage(T messageSendEvent, BaseSendExtendDTO requestParam);

    /**
     * 同步发送消息
     *
     * @param messageSendEvent 待发送的消息事件对象，T为泛型，表示消息体的具体类型
     * @return 消息发送结果，包含发送状态、消息ID等信息
     */
    public SendResult sendMessage(T messageSendEvent) {
        // 1. 构建基础发送扩展参数DTO
        // 该DTO封装了发送消息所需的元数据，如主题(topic)、标签(tag)、发送超时时间、业务键(keys)等
        BaseSendExtendDTO baseSendExtendDTO = buildBaseSendExtendParam(messageSendEvent);

        // 2. 声明消息发送结果变量，用于接收同步发送后的返回结果
        SendResult sendResult;

        try {
            // 3. 使用字符串构建器拼接RocketMQ的目的地地址（由主题和标签组成）
            // StrUtil.builder()是Hutool工具类提供的高效字符串构建方式
            StringBuilder destinationBuilder = StrUtil.builder().append(baseSendExtendDTO.getTopic());

            // 4. 判断标签(tag)是否不为空且不为空白字符串
            if (StrUtil.isNotBlank(baseSendExtendDTO.getTag())) {
                // 5. 如果标签存在，则拼接标签（RocketMQ中主题和标签用冒号":"分隔）
                destinationBuilder.append(":").append(baseSendExtendDTO.getTag());
            }

            // 6. 调用RocketMQ模板的同步发送方法发送消息
            // 参数1: 目的地地址(topic:tag)
            // 参数2: 构建后的消息对象，包含消息体和可能的附加属性
            // 参数3: 发送超时时间，从DTO中获取
            sendResult = rocketMQTemplate.syncSend(destinationBuilder.toString(),
                    buildMessage(messageSendEvent, baseSendExtendDTO), baseSendExtendDTO.getSentTimeout());

            // 7. 打印消息发送成功日志
            // 日志包含事件名称、发送状态、消息ID和业务键，便于追踪和问题排查
            log.info("[{}] 消息发送结果：{}，消息ID：{}，消息Keys：{}", baseSendExtendDTO.getEventName(), sendResult.getSendStatus(),
                    sendResult.getMsgId(), baseSendExtendDTO.getKeys());
        } catch (Throwable ex) {
            // 8. 捕获所有可能发生的异常（包括检查型异常和非检查型异常）
            // 这是一个异常安全的做法，确保任何发送失败都能被捕获

            // 9. 打印消息发送失败日志
            // 日志包含事件名称、消息体（转为JSON字符串）和完整的异常堆栈信息
            log.error("[{}] 消息发送失败，消息体：{}", baseSendExtendDTO.getEventName(), JSON.toJSONString(messageSendEvent), ex);

            // 10. 将捕获到的异常向上抛出，由调用方决定如何处理失败情况
            // 例如，调用方可能会进行重试、记录失败日志或触发告警
            throw ex;
        }

        // 11. 返回消息发送结果给调用方
        return sendResult;
    }
}
