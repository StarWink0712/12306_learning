package com.guoxu.payservice.mq.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

/**
 * PayResultCallbackOrderEvent
 * 支付结果回调订单事件
 * @author 执笔画棠
 * @date 2025/11/11 18:21
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
public final class PayResultCallbackOrderEvent {

    /**
     * id
     */
    private Long id;

    /**
     * 订单号
     */
    private String orderSn;

    /**
     * 商户订单号
     */
    private String outOrderSn;

    /**
     * 支付渠道
     */
    private String channel;

    /**
     * 支付环境
     */
    private String tradeType;

    /**
     * 订单标题
     */
    private String subject;

    /**
     * 交易凭证号
     */
    private String tradeNo;

    /**
     * 商户订单号
     * 由商家自定义，64个字符以内，仅支持字母、数字、下划线且需保证在商户端不重复
     */
    private String orderRequestId;

    /**
     * 交易总金额
     */
    private BigDecimal totalAmount;

    /**
     * 付款时间
     */
    private Date gmtPayment;

    /**
     * 支付金额
     */
    private BigDecimal payAmount;

    /**
     * 支付状态
     */
    private String status;
}
