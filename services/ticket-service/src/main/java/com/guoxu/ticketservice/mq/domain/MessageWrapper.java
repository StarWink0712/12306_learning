package com.guoxu.ticketservice.mq.domain;

import cn.hutool.core.lang.UUID;
import lombok.*;

import java.io.Serializable;

/**
 * MessageWrapper
 *
 * @author 执笔画棠
 * @date 2025/11/12 22:11
 **/
@Data
@Builder
@NoArgsConstructor(force = true)
@AllArgsConstructor
@RequiredArgsConstructor
public final class MessageWrapper<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 消息发送 Keys
     */
    @NonNull
    private String keys;

    /**
     * 消息体
     */
    @NonNull
    private T message;

    /**
     * 唯一标识，用于客户端幂等验证
     */
    private String uuid = UUID.randomUUID().toString();

    /**
     * 消息发送时间
     */
    private Long timestamp = System.currentTimeMillis();
}
