package com.guoxu.orderservice.common.enums;

import cn.crane4j.annotation.ContainerEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * OrderItemStatusEnum
 * 订单项状态枚举类
 * @author 执笔画棠
 * @version 2025/11/09 19:50
 **/
/*主要用于将枚举容器化，便于在系统中统一管理和使用枚举值。
namespace: 定义枚举在容器中的命名空间，这里为 "OrderItemStatusEnum"

key: 指定枚举中作为键的字段，这里为 "status"（对应 Integer status 字段）

value: 指定枚举中作为值的字段，这里为 "statusName"（对应 String statusName 字段）
 */
@ContainerEnum(namespace = "OrderItemStatusEnum", key = "status", value = "statusName")
@RequiredArgsConstructor
public enum OrderItemStatusEnum {
    /**
     * 待支付
     */
    PENDING_PAYMENT(0, "待支付"),

    /**
     * 已支付
     */
    ALREADY_PAID(10, "已支付"),

    /**
     * 已进站
     */
    ALREADY_PULL_IN(20, "已进站"),

    /**
     * 已取消
     */
    CLOSED(30, "已取消"),

    /**
     * 已退票
     */
    REFUNDED(40, "已退票"),

    /**
     * 已改签
     */
    RESCHEDULED(50, "已改签");

    @Getter
    private final Integer status;

    @Getter
    private final String statusName;
}