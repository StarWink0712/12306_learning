package com.guoxu.payservice.dto.base;

import lombok.Getter;
import lombok.Setter;

/**
 * AbstractPayCallbackRequest
 * 抽象支付回调入参实体
 * @author 执笔画棠
 * @date 2025/11/11 14:48
 **/
public abstract class AbstractPayCallbackRequest implements PayCallbackRequest{

    @Getter
    @Setter
    private String orderRequestId;

    @Override
    public AliPayCallbackRequest getAliPayCallBackRequest() {
        return null;
    }

    @Override
    public String buildMark() {
        return null;
    }
}
