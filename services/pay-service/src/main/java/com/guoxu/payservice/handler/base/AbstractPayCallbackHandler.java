package com.guoxu.payservice.handler.base;

import com.guoxu.payservice.dto.base.PayCallbackRequest;

/**
 * AbstractPayCallbackHandler
 * 抽象支付回调组件
 * @author 执笔画棠
 * @date 2025/11/11 18:08
 **/
public abstract class AbstractPayCallbackHandler {

    /**
     * 支付回调抽象接口
     *
     * @param payCallbackRequest 支付回调请求参数
     */
    public abstract void callback(PayCallbackRequest payCallbackRequest);
}
