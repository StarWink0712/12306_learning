package com.guoxu.orderservice.mq.domain;

import cn.hutool.core.lang.UUID;
import lombok.*;

import java.io.Serializable;

/**
 * MessageWrapper 消息体包装器
 * 用于封装消息体，包含消息的主题、标签、键值对等信息
 * T是消息体，其他的是通用的消息信息
 * @author 执笔画棠
 * @date 2025/11/10 18:16
 **/
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@RequiredArgsConstructor
public final class MessageWrapper<T> implements Serializable {

    private static final long serialVersionUID = 1L;


    //消息发送keys
    @NonNull
    private String keys;

    //消息体
    @NonNull
    private T message;

    //唯一标识
    private String uuid= UUID.randomUUID().toString();

    //消息发送时间
    private long timestamp=System.currentTimeMillis();
}
