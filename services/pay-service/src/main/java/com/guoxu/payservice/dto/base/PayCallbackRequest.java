package com.guoxu.payservice.dto.base;

/**
 * PayCallbackRequest 支付回调请求入参接口
 *
 * @author 执笔画棠
 * @version 2025/11/11 17:39
 **/
public interface PayCallbackRequest {

    /**
     * 获取阿里支付回调入参
     */
    AliPayCallbackRequest getAliPayCallBackRequest();

    /**
     * 构建查找支付回调策略实现类标识
     */
    String buildMark();
}