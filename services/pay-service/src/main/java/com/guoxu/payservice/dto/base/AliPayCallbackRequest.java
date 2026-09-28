package com.guoxu.payservice.dto.base;

import com.guoxu.payservice.common.enums.PayChannelEnum;
import lombok.Data;

import java.util.Date;

/**
 * AliPayCallbackRequest
 * 支付宝支付回调入参实体
 * @author 执笔画棠
 * @date 2025/11/11 17:42
 **/
@Data
public abstract class AliPayCallbackRequest extends AbstractPayCallbackRequest {
    /**
     * 支付渠道
     */
    private String channel;

    /**
     * 支付状态
     */
    private String tradeStatus;

    /**
     * 支付凭证号
     */
    private String tradeNo;

    /**
     * 买家付款时间
     */
    private Date gmtPayment;

    /**
     * 买家付款金额
     */
    private Integer buyerPayAmount;

    @Override
    public AliPayCallbackRequest getAliPayCallBackRequest() {
        return this;
    }

    @Override
    public String buildMark() {
        return PayChannelEnum.ALI_PAY.getName();
    }
}
