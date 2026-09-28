package com.guoxu.ticketservice.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * TicketStatusEnum
 * 车票状态枚举
 * @author 执笔画棠
 * @version 2025/11/12 20:13
 **/
@RequiredArgsConstructor
public enum TicketStatusEnum {

    /**
     * 未支付
     */
    UNPAID(0),

    /**
     * 已支付
     */
    PAID(1),

    /**
     * 已进站
     */
    BOARDED(2),

    /**
     * 改签
     */
    CHANGED(3),

    /**
     * 退票
     */
    REFUNDED(4),

    /**
     * 已取消
     */
    CLOSED(5);

    @Getter
    private final Integer code;
}