package com.guoxu.payservice.handler;

import com.guoxu.payservice.dto.base.RefundRequest;
import com.guoxu.payservice.dto.base.RefundResponse;
import com.guoxu.payservice.handler.base.AbstractRefundHandler;
import com.guoxu.strategy.AbstractExecuteStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * AliRefundNativeHandler 阿里支付组件退款
 *
 * @author 执笔画棠
 * @date 2025/11/11 18:14
 **/
@Service
@Slf4j
@RequiredArgsConstructor
public class AliRefundNativeHandler extends AbstractRefundHandler
        implements AbstractExecuteStrategy<RefundRequest, RefundResponse> {

    // 注入AliPayProperties，用于获取支付宝相关的配置属性
    private final AliPayProperties aliPayProperties;

    // 定义常量SUCCESS_CODE，表示支付宝退款成功的响应码
    private final static String SUCCESS_CODE = "10000";

    // 定义常量FUND_CHANGE，表示资金发生变化的标识
    private final static String FUND_CHANGE = "Y";

    // 定义重试注解，当发生ServiceException异常时，最多重试3次
    // 每次重试间隔1000毫秒，并以1.5的倍数递增间隔时间
    @Retryable(value = { ServiceException.class }, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 1.5))
    // 注解用于忽略AlipayApiException异常，将其包装成运行时异常抛出
    @SneakyThrows(value = AlipayApiException.class)
    // 重写refund方法，执行支付宝退款操作
    @Override
    public RefundResponse refund(RefundRequest payRequest) {
        // 从退款请求中获取支付宝退款请求对象
        AliRefundRequest aliRefundRequest = payRequest.getAliRefundRequest();
        // 将AliPayProperties转换为AlipayConfig对象，用于配置支付宝客户端
        AlipayConfig alipayConfig = BeanUtil.convert(aliPayProperties, AlipayConfig.class);
        // 创建支付宝客户端实例
        AlipayClient alipayClient = new DefaultAlipayClient(alipayConfig);
        // 创建支付宝退款模型对象
        AlipayTradeRefundModel model = new AlipayTradeRefundModel();
        // 设置退款模型中的外部订单号
        model.setOutTradeNo(aliRefundRequest.getOrderSn());
        // 设置退款模型中的交易号
        model.setTradeNo(aliRefundRequest.getTradeNo());
        // 获取退款请求中的支付金额
        BigDecimal payAmount = aliRefundRequest.getPayAmount();
        // 将支付金额除以100，转换为以元为单位的退款金额
        BigDecimal refundAmount = payAmount.divide(new BigDecimal(100));
        // 设置退款模型中的退款金额
        model.setRefundAmount(refundAmount.toString());
        // 设置退款模型中的外部请求号，使用雪花算法生成唯一ID字符串
        model.setOutRequestNo(SnowflakeIdUtil.nextIdStr());
        // 创建支付宝退款请求对象
        AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();
        // 将退款模型设置到退款请求中
        request.setBizModel(model);
        try {
            // 执行支付宝退款请求，获取退款响应
            AlipayTradeRefundResponse response = alipayClient.execute(request);
            // 将退款响应转换为JSON字符串
            String responseJson = JSONObject.toJSONString(response);
            // 记录日志，打印发起退款的订单号、交易凭证号、退款金额以及退款响应信息
            log.info("发起支付宝退款，订单号：{}，交易凭证号：{}，退款金额：{} \n调用退款响应：\n\n{}\n",
                    aliRefundRequest.getOrderSn(),
                    aliRefundRequest.getTradeNo(),
                    aliRefundRequest.getPayAmount(),
                    responseJson);
            // 如果响应码不等于SUCCESS_CODE或者资金未发生变化
            if (!StrUtil.equals(SUCCESS_CODE, response.getCode())
                    || !StrUtil.equals(FUND_CHANGE, response.getFundChange())) {
                // 抛出业务异常，表示退款失败
                throw new ServiceException("退款失败");
            }
            // 返回退款响应对象，包含交易关闭状态码和交易号
            return new RefundResponse(TradeStatusEnum.TRADE_CLOSED.tradeCode(), response.getTradeNo());
        } catch (AlipayApiException e) {
            // 如果发生支付宝API异常，抛出业务异常，表示调用支付宝退款异常
            throw new ServiceException("调用支付宝退款异常");
        }
    }

    // 重写mark方法，返回标识字符串，包含支付渠道、支付交易类型和交易状态码
    @Override
    public String mark() {
        return StrBuilder.create()
                .append(PayChannelEnum.ALI_PAY.name())
                .append("_")
                .append(PayTradeTypeEnum.NATIVE.name())
                .append("_")
                .append(TradeStatusEnum.TRADE_CLOSED.tradeCode())
                .toString();
    }

    // 实现executeResp方法，直接调用refund方法执行退款操作并返回退款响应
    @Override
    public RefundResponse executeResp(RefundRequest requestParam) {
        return refund(requestParam);
    }
}
