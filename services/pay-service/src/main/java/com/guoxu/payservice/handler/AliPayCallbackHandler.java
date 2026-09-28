package com.guoxu.payservice.handler;

import com.guoxu.payservice.common.enums.PayChannelEnum;
import com.guoxu.payservice.common.enums.TradeStatusEnum;
import com.guoxu.payservice.dto.PayCallbackReqDTO;
import com.guoxu.payservice.dto.base.AliPayCallbackRequest;
import com.guoxu.payservice.dto.base.PayCallbackRequest;
import com.guoxu.payservice.handler.base.AbstractPayCallbackHandler;
import com.guoxu.payservice.service.PayService;
import com.guoxu.strategy.AbstractExecuteStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * AliPayCallbackHandler
 *
 * @author 执笔画棠
 * @date 2025/11/11 14:50
 **/
@Service
@Slf4j
@RequiredArgsConstructor
public class AliPayCallbackHandler extends AbstractPayCallbackHandler
        implements AbstractExecuteStrategy<PayCallbackRequest, Void> {

    // 注入PayService，用于调用支付相关业务方法
    private final PayService payService;

    // 重写callback方法，处理支付回调逻辑
    @Override
    public void callback(PayCallbackRequest payCallbackRequest) {
        // 从支付回调请求中获取支付宝支付回调请求对象
        AliPayCallbackRequest aliPayCallBackRequest = payCallbackRequest.getAliPayCallBackRequest();
        // 使用Builder模式构建PayCallbackReqDTO对象，将支付宝回调请求中的相关信息设置进去
        PayCallbackReqDTO payCallbackRequestParam = PayCallbackReqDTO.builder()
                // 根据支付宝回调的交易状态查询实际的交易状态码并设置
                .status(TradeStatusEnum.queryActualTradeStatusCode(aliPayCallBackRequest.getTradeStatus()))
                // 设置买家支付金额
                .payAmount(aliPayCallBackRequest.getBuyerPayAmount())
                // 设置交易号
                .tradeNo(aliPayCallBackRequest.getTradeNo())
                // 设置支付时间
                .gmtPayment(aliPayCallBackRequest.getGmtPayment())
                // 设置订单号
                .orderSn(aliPayCallBackRequest.getOrderRequestId())
                // 构建PayCallbackReqDTO对象
                .build();
        // 调用PayService的callbackPay方法，传入构建好的支付回调请求参数，处理支付回调业务
        // 最后就是进到这
        payService.callbackPay(payCallbackRequestParam);
    }

    // 重写mark方法，返回支付渠道标识为“ALI_PAY”
    @Override
    public String mark() {
        return PayChannelEnum.ALI_PAY.name();
    }

    // 实现execute方法，直接调用callback方法处理支付回调请求
    public void execute(PayCallbackRequest requestParam) {
        callback(requestParam);
    }
}
