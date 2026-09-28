package com.guoxu.payservice.mq.domain;

import cn.hutool.core.lang.UUID;
import lombok.*;

import java.io.Serializable;

/**
 * MessageWrapper
 * 消息体包装器
 * @author 执笔画棠
 * @date 2025/11/11 18:19
 **/
@Data
@Builder
@AllArgsConstructor
// 强制生成无参构造函数
@NoArgsConstructor(force = true)
// 生成包含所有 final 字段的构造函数
@RequiredArgsConstructor
public class MessageWrapper<T> implements Serializable {
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
