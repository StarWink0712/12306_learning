package com.guoxu.payservice.dto;

import com.guoxu.payservice.dto.base.AbstractPayRequest;
import lombok.Data;

import java.math.BigDecimal;

/**
 * PayCommand
 * 支付请求命令
 * @author 执笔画棠
 * @date 2025/11/11 18:00
 **/
@Data
public final class PayCommand extends AbstractPayRequest {
    /**
     * 子订单号
     */
    private String outOrderSn;

    /**
     * 订单总金额
     * 单位为元，精确到小数点后两位，取值范围：[0.01,100000000]
     */
    private BigDecimal totalAmount;

    /**
     * 订单标题
     * 注意：不可使用特殊字符，如 /，=，& 等
     */
    private String subject;
}
