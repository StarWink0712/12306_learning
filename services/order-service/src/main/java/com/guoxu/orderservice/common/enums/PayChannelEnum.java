package com.guoxu.orderservice.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * PayChannelEnum 支付渠道枚举
 *
 * @author 执笔画棠
 * @version 2025/11/09 20:25
 **/
@RequiredArgsConstructor
public enum PayChannelEnum {
    /**
     * 支付宝
     */
    ALI_PAY(0, "ALI_PAY", "支付宝");

    @Getter
    private final Integer code;

    @Getter
    private final String name;

    @Getter
    private final String value;

}