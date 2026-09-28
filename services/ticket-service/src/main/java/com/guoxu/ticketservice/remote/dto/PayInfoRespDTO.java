package com.guoxu.ticketservice.remote.dto;

import lombok.Data;

import java.util.Date;

/**
 * PayInfoRespDTO
 * 支付单详情信息返回参数
 * @author 执笔画棠
 * @date 2025/11/12 22:18
 **/
@Data
public class PayInfoRespDTO {
    /**
     * 订单号
     */
    private String orderSn;

    /**
     * 支付总金额
     */
    private Integer totalAmount;

    /**
     * 支付状态
     */
    private Integer status;

    /**
     * 支付时间
     */
    private Date gmtPayment;
}
