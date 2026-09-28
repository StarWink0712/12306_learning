package com.guoxu.payservice.convent;

import com.guoxu.payservice.common.enums.PayChannelEnum;
import com.guoxu.payservice.dto.PayCommand;
import com.guoxu.payservice.dto.base.AliPayRequest;
import com.guoxu.payservice.dto.base.PayRequest;
import com.guoxu.toolkit.BeanUtil;

import java.util.Objects;

/**
 * PayRequestConvert
 * 支付请求入参转换器
 * @author 执笔画棠
 * @date 2025/11/11 19:59
 **/
public final class PayRequestConvert {

    /**
     * {@link PayCommand} to {@link PayRequest}
     *
     * @param payCommand 支付请求参数
     * @return {@link PayRequest}
     */
    public static PayRequest command2PayRequest(PayCommand payCommand) {
        PayRequest payRequest = null;
        if (Objects.equals(payCommand.getChannel(), PayChannelEnum.ALI_PAY.getCode())) {
            payRequest = BeanUtil.convert(payCommand, AliPayRequest.class);
        }
        return payRequest;
    }
}
