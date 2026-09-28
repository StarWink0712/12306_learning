package com.guoxu.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Objects;

/**
 * IdempotentMQConsumeStatusEnum 幂等消费状态枚举
 *
 * @author 执笔画棠
 * @version 2025/11/06 20:41
 **/
@RequiredArgsConstructor
public enum IdempotentMQConsumeStatusEnum {
    /**
     * 消费中
     */
    CONSUMING("0"),

    /**
     * 已消费
     */
    CONSUMED("1");

    @Getter
    private final String code;

    /**
     * 如果消费状态等于消费中，返回失败
     *
     * @param consumeStatus 消费状态
     * @return 是否消费失败
     */
    public static boolean isError(String consumeStatus) {
        return Objects.equals(CONSUMING.code, consumeStatus);
    }

}