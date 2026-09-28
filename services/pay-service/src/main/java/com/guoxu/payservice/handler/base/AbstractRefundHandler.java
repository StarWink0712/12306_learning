package com.guoxu.payservice.handler.base;

import com.guoxu.payservice.dto.base.RefundRequest;
import com.guoxu.payservice.dto.base.RefundResponse;

/**
 * AbstractRefundHandler
 * 抽象退款组件
 * @author 执笔画棠
 * @date 2025/11/11 18:09
 **/
public abstract class AbstractRefundHandler {
    /**
     * 支付退款接口
     *
     * @param payRequest 退款请求参数
     * @return 退款响应参数
     */
    public abstract RefundResponse refund(RefundRequest payRequest);
}
