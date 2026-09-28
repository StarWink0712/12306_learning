package com.guoxu.payservice.handler;

import cn.hutool.core.text.StrBuilder;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.AlipayConfig;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.domain.AlipayTradePagePayModel;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.response.AlipayTradePagePayResponse;
import com.guoxu.exception.ServiceException;
import com.guoxu.payservice.common.enums.PayChannelEnum;
import com.guoxu.payservice.common.enums.PayTradeTypeEnum;
import com.guoxu.payservice.config.AliPayProperties;
import com.guoxu.payservice.dto.base.AliPayRequest;
import com.guoxu.payservice.dto.base.PayRequest;
import com.guoxu.payservice.dto.base.PayResponse;
import com.guoxu.payservice.handler.base.AbstractPayHandler;
import com.guoxu.strategy.AbstractExecuteStrategy;
import com.guoxu.toolkit.BeanUtil;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

/**
 * AliPayNativeHandler 阿里支付组件
 * 实现了策略执行接口，这个是重点，所以需要执行策略模式的都要实现这个接口
 * @author 执笔画棠
 * @date 2025/11/11 18:13
 **/
@Slf4j
@Service
@RequiredArgsConstructor
// 支付宝原生支付处理器类，继承抽象支付处理器并实现策略执行接口
public class AliPayNativeHandler extends AbstractPayHandler
        implements AbstractExecuteStrategy<PayRequest, PayResponse> {

    // 支付宝支付配置属性
    private final AliPayProperties aliPayProperties;

    // 支付方法，使用@SneakyThrows自动抛出AlipayApiException异常
    // 使用@Retryable注解实现重试机制：当发生ServiceException时最多重试3次，每次重试间隔1000毫秒，间隔时间
    // multiplier为1.5倍递增
    @SneakyThrows(value = AlipayApiException.class)
    @Override
    @Retryable(value = ServiceException.class, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 1.5))
    public PayResponse pay(PayRequest payRequest) {
        // 从支付请求中获取支付宝特定请求参数
        AliPayRequest aliPayRequest = payRequest.getAliPayRequest();
        // 将配置属性转换为支付宝SDK需要的配置对象
        AlipayConfig alipayConfig = BeanUtil.convert(aliPayProperties, AlipayConfig.class);
        // 创建支付宝客户端实例
        AlipayClient alipayClient = new DefaultAlipayClient(alipayConfig);

        // 创建支付宝页面支付模型并设置参数
        AlipayTradePagePayModel model = new AlipayTradePagePayModel();
        // 设置商户订单号
        model.setOutTradeNo(aliPayRequest.getOrderSn());
        // 设置订单总金额
        model.setTotalAmount(aliPayRequest.getTotalAmount().toString());
        // 设置订单标题
        model.setSubject(aliPayRequest.getSubject());
        // 设置产品码，FAST_INSTANT_TRADE_PAY表示快速即时到账交易
        model.setProductCode("FAST_INSTANT_TRADE_PAY");

        // 创建支付宝页面支付请求
        AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
        // 设置异步通知回调地址，用于支付宝支付结果通知，商户需要实现该URL接收支付宝支付结果通知
        // 即PayServiceImpl中的callback方法，用于处理支付宝支付结果通知
        request.setNotifyUrl(aliPayProperties.getNotifyUrl());
        // 设置业务请求参数
        request.setBizModel(model);

        try {
            // 执行支付宝页面支付请求，获取响应
            AlipayTradePagePayResponse response = alipayClient.pageExecute(request);
            // 记录支付请求日志，包含订单信息和支付宝返回的完整响应
            log.info("发起支付宝支付，订单号：{}，子订单号：{}，订单请求号：{}，订单金额：{} \n调用支付返回：\n\n{}\n",
                    aliPayRequest.getOrderSn(),
                    aliPayRequest.getOutOrderSn(),
                    aliPayRequest.getOrderRequestId(),
                    aliPayRequest.getTotalAmount(),
                    JSONObject.toJSONString(response));

            // 检查支付宝响应是否成功
            if (!response.isSuccess()) {
                // 如果支付宝返回失败，抛出业务异常
                throw new ServiceException("调用支付宝发起支付异常");
            }
            // 构建支付响应，对支付宝返回的body进行处理：替换双引号为单引号，移除换行符
            // response.getBody() 是支付宝返回的原始支付页面HTML内容
            // 对其进行处理，替换双引号为单引号，移除换行符，确保JSON格式的兼容性
            return new PayResponse(StrUtil.replace(StrUtil.replace(response.getBody(), "\"", "'"), "\n", ""));
        } catch (AlipayApiException ex) {
            // 捕获支付宝API异常，转换为业务异常抛出
            throw new ServiceException("调用支付宝支付异常");
        }
    }

    // 实现策略接口的mark方法，返回策略标识
    @Override
    public String mark() {
        // 构建策略标识字符串：支付渠道_交易类型
        return StrBuilder.create()
                .append(PayChannelEnum.ALI_PAY.name()) // 添加支付渠道：ALI_PAY
                .append("_") // 添加分隔符
                .append(PayTradeTypeEnum.NATIVE.name()) // 添加交易类型：NATIVE
                .toString(); // 转换为字符串
    }

    // 实现策略执行接口的executeResp方法
    @Override
    public PayResponse executeResp(PayRequest requestParam) {
        // 直接调用pay方法处理支付请求
        return pay(requestParam);
    }
}
