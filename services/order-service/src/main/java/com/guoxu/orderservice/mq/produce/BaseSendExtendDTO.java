package com.guoxu.orderservice.mq.produce;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * BaseSendExtendDTO
 *
 * @author 执笔画棠
 * @date 2025/11/10 19:44
 **/
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public final class BaseSendExtendDTO {
    /**
     * 事件名称
     */
    private String eventName;

    /**
     * 主题
     */
    private String topic;

    /**
     * 标签
     */
    private String tag;

    /**
     * 业务标识
     */
    private String keys;

    /**
     * 发送消息超时时间
     */
    private Long sentTimeout;

    /**
     * 延迟消息
     */
    private Integer delayLevel;
}
