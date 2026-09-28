package com.guoxu.payservice.mq.produce;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * BaseSendExtendDTO
 * 消息发送事件基础扩充属性实体
 * @author 执笔画棠
 * @date 2025/11/11 18:24
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
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
}
