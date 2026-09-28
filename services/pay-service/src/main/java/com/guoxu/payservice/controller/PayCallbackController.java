package com.guoxu.payservice.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.date.DateUtil;
import com.guoxu.payservice.common.enums.PayChannelEnum;
import com.guoxu.payservice.convent.PayCallbackRequestConvert;
import com.guoxu.payservice.dto.PayCallbackCommand;
import com.guoxu.payservice.dto.base.PayCallbackRequest;
import com.guoxu.strategy.AbstractStrategyChoose;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * PayCallbackController 支付结果回调
 *
 * @author 执笔画棠
 * @date 2025/11/11 17:27
 **/
@RestController
@RequiredArgsConstructor
public class PayCallbackController {

    // 策略选择器，在framework的designpattern包下的AbstractStrategyChoose类
    private final AbstractStrategyChoose abstractStrategyChoose;

    /**
     * 支付宝回调
     * 调用支付宝支付后，支付宝会调用此接口发送支付结果
     * 前端支付完后，支付宝服务器会发送异步post请求到该接口，携带支付结果参数
     * 商户需要在该接口中解析请求参数，验证签名，确认支付结果是否有效
     * 如果支付结果有效，商户需要更新订单状态为已支付，并返回成功响应给支付宝
     * 如果支付结果无效，商户需要返回失败响应给支付宝
     */
    @PostMapping("/api/pay-service/callback/alipay")
    public void callbackAlipay(@RequestParam Map<String, Object> requestParam) {
        PayCallbackCommand payCallbackCommand = BeanUtil.mapToBean(requestParam, PayCallbackCommand.class, true,
                CopyOptions.create());
        payCallbackCommand.setChannel(PayChannelEnum.ALI_PAY.getCode());
        payCallbackCommand.setOrderRequestId(requestParam.get("out_trade_no").toString());
        payCallbackCommand.setGmtPayment(DateUtil.parse(requestParam.get("gmt_payment").toString()));
        PayCallbackRequest payCallbackRequest = PayCallbackRequestConvert
                .command2PayCallbackRequest(payCallbackCommand);
        /**
         * {@link AliPayCallbackHandler}
         */
        // 策略模式：通过策略模式封装支付回调渠道，支付回调时动态选择对应的支付回调组件
        // 这里是回调信息中的渠道标识，选择对应的支付回调组件，即选AliPayCallbackHandler
        abstractStrategyChoose.chooseAndExecute(payCallbackRequest.buildMark(), payCallbackRequest);
    }
}
