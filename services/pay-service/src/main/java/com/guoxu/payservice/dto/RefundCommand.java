package com.guoxu.payservice.dto;

import com.guoxu.payservice.dto.base.AbstractRefundRequest;
import lombok.Data;

import java.math.BigDecimal;

/**
 * RefundCommand
 * 退款请求命令
 * @author 执笔画棠
 * @date 2025/11/11 18:02
 **/
@Data
public final class RefundCommand extends AbstractRefundRequest {
    /**
     * 支付金额
     */
    private BigDecimal payAmount;

    /**
     * 交易凭证号
     */
    private String tradeNo;
}
