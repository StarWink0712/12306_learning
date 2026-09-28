package com.guoxu.payservice.dto.base;

/**
 * RefundRequest 退款入参接口
 *
 * @author 执笔画棠
 * @version 2025/11/11 17:49
 **/
public interface RefundRequest {
    /**
     * 获取阿里退款入参
     */
    AliRefundRequest getAliRefundRequest();

    /**
     * 获取订单号
     */
    String getOrderSn();

    /**
     * 构建查找支付策略实现类标识
     */
    String buildMark();
}