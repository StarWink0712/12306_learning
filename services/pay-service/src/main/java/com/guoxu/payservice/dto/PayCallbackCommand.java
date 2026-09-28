package com.guoxu.payservice.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.guoxu.payservice.dto.base.AbstractPayRequest;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * PayCallbackCommand
 * 支付回调请求命令
 * @author 执笔画棠
 * @date 2025/11/11 14:49
 **/
@Data
public final class PayCallbackCommand extends AbstractPayRequest {

    /**
     * 支付渠道
     */
    private Integer channel;

    /**
     * 支付状态
     * JsonAlias注解用于指定JSON字段的别名，当JSON字段与Java字段名不一致时使用
     */
    @JsonAlias("trade_status")
    private String tradeStatus;

    /**
     * 支付凭证号
     */
    @JsonAlias("trade_no")
    private String tradeNo;

    /**
     * 买家付款时间
     */
    @JsonAlias("gmt_payment")
    private Date gmtPayment;

    /**
     * 买家付款金额
     */
    @JsonAlias("buyer_pay_amount")
    private BigDecimal buyerPayAmount;

    /**
     * 商户订单号
     * 由商家自定义，64个字符以内，仅支持字母、数字、下划线且需保证在商户端不重复
     */
    @JsonAlias("out_trade_no")
    private String outTradeNo;

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
