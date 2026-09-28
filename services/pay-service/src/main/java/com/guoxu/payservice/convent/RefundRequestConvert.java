package com.guoxu.payservice.convent;

import com.guoxu.payservice.common.enums.PayChannelEnum;
import com.guoxu.payservice.dto.RefundCommand;
import com.guoxu.payservice.dto.base.AliRefundRequest;
import com.guoxu.payservice.dto.base.RefundRequest;
import com.guoxu.toolkit.BeanUtil;

import java.util.Objects;

/**
 * RefundRequestConvert 退款请求入参转换器
 *
 * @author 执笔画棠
 * @date 2025/11/11 20:00
 **/
public final class RefundRequestConvert {

    /**
     * {@link RefundCommand} to {@link RefundRequest}
     *
     * @param refundCommand 退款请求参数
     * @return {@link RefundRequest}
     */
    public static RefundRequest command2RefundRequest(RefundCommand refundCommand) {
        RefundRequest refundRequest = null;
        if (Objects.equals(refundCommand.getChannel(), PayChannelEnum.ALI_PAY.getCode())) {
            refundRequest = BeanUtil.convert(refundCommand, AliRefundRequest.class);
        }
        return refundRequest;
    }
}
