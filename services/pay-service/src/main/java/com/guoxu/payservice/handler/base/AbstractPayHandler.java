package com.guoxu.payservice.handler.base;

import com.guoxu.payservice.dto.base.PayRequest;
import com.guoxu.payservice.dto.base.PayResponse;

/**
 * AbstractPayHandler 抽象支付组件
 *
 * @author 执笔画棠
 * @date 2025/11/11 18:09
 **/
public abstract class AbstractPayHandler {
    /**
     * 支付抽象接口
     *
     * @param payRequest 支付请求参数
     * @return 支付响应参数
     */
    public abstract PayResponse pay(PayRequest payRequest);
}
