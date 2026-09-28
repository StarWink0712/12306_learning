package com.guoxu.payservice.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoxu.base.BaseDo;
import lombok.Data;

import java.util.Date;

/**
 * PayDO 支付实体
 *
 * @author 执笔画棠
 * @date 2025/11/11 19:54
 **/
@Data
@TableName("t_pay")
public class PayDO extends BaseDo {
    /**
     * id
     */
    private Long id;

    /**
     * 支付流水号
     */
    private String paySn;

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
    private Integer channel;

    /**
     * 支付环境
     */
    private Integer tradeType;

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
    private Integer totalAmount;

    /**
     * 付款时间
     */
    private Date gmtPayment;

    /**
     * 支付金额
     */
    private Integer payAmount;

    /**
     * 支付状态
     */
    private Integer status;
}
