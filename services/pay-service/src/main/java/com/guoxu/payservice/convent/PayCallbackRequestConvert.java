package com.guoxu.payservice.convent;

import com.guoxu.payservice.common.enums.PayChannelEnum;
import com.guoxu.payservice.dto.PayCallbackCommand;
import com.guoxu.payservice.dto.base.AliPayCallbackRequest;
import com.guoxu.payservice.dto.base.PayCallbackRequest;
import com.guoxu.toolkit.BeanUtil;

import java.util.Objects;

/**
 * PayCallbackRequestConvert
 * 支付回调请求入参转换器
 * @author 执笔画棠
 * @date 2025/11/11 19:58
 **/

public final class PayCallbackRequestConvert {
    /**
     * 将PayCallbackCommand转换为PayCallbackRequest
     *
     * @param payCallbackCommand 支付回调命令对象，包含支付回调相关参数
     * @return PayCallbackRequest对象，如果支付渠道是支付宝则返回AliPayCallbackRequest对象，否则返回null
     */
    public static PayCallbackRequest command2PayCallbackRequest(PayCallbackCommand payCallbackCommand) {
        // 初始化支付回调请求对象为null
        PayCallbackRequest payCallbackRequest = null;
        // 判断支付回调命令中的支付渠道是否为支付宝
        if (Objects.equals(payCallbackCommand.getChannel(), PayChannelEnum.ALI_PAY.getCode())) {
            // 如果是支付宝，将PayCallbackCommand转换为AliPayCallbackRequest对象
            payCallbackRequest = BeanUtil.convert(payCallbackCommand, AliPayCallbackRequest.class);
            // 设置AliPayCallbackRequest对象中的订单请求ID
            ((AliPayCallbackRequest) payCallbackRequest).setOrderRequestId(payCallbackCommand.getOrderRequestId());
        }
        // 返回转换后的支付回调请求对象
        return payCallbackRequest;
    }
}
