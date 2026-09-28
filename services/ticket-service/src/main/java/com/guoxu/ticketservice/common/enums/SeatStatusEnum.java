package com.guoxu.ticketservice.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * SeatStatusEnum
 * 座位状态枚举
 * @author 执笔画棠
 * @version 2025/11/12 20:03
 **/
@RequiredArgsConstructor
public enum SeatStatusEnum {
    /**
     * 可售
     */
    AVAILABLE(0),

    /**
     * 锁定
     */
    LOCKED(1),

    /**
     * 已售
     */
    SOLD(2);

    @Getter
    private final Integer code;
}