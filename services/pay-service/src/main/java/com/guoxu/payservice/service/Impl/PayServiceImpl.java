package com.guoxu.payservice.service.Impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.guoxu.DistributedCache;
import com.guoxu.annotation.Idempotent;
import com.guoxu.enums.IdempotentTypeEnum;
import com.guoxu.exception.ServiceException;
import com.guoxu.payservice.common.enums.TradeStatusEnum;
import com.guoxu.payservice.convent.RefundRequestConvert;
import com.guoxu.payservice.dao.entity.PayDO;
import com.guoxu.payservice.dao.mapper.PayMapper;
import com.guoxu.payservice.dto.*;
import com.guoxu.payservice.dto.base.PayRequest;
import com.guoxu.payservice.dto.base.PayResponse;
import com.guoxu.payservice.dto.base.RefundRequest;
import com.guoxu.payservice.dto.base.RefundResponse;
import com.guoxu.payservice.mq.event.PayResultCallbackOrderEvent;
import com.guoxu.payservice.mq.produce.PayResultCallbackOrderSendProduce;
import com.guoxu.payservice.service.PayService;
import com.guoxu.payservice.service.payid.PayIdGeneratorManager;
import com.guoxu.strategy.AbstractStrategyChoose;
import com.guoxu.toolkit.BeanUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serial;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static com.guoxu.payservice.common.constant.RedisKeyConstant.ORDER_PAY_RESULT_INFO;

/**
 * PayServiceImpl
 *
 * @author 执笔画棠
 * @date 2025/11/11 19:52
 **/
@Slf4j
@Service
@RequiredArgsConstructor
public class PayServiceImpl implements PayService {
    // 注入PayMapper，用于数据库中支付相关数据的持久化操作
    private final PayMapper payMapper;
    // 注入AbstractStrategyChoose，用于实现支付和退款策略的动态选择
    private final AbstractStrategyChoose abstractStrategyChoose;
    // 注入PayResultCallbackOrderSendProduce，用于发送支付结果回调订单的消息
    private final PayResultCallbackOrderSendProduce payResultCallbackOrderSendProduce;
    // 注入DistributedCache，用于操作分布式缓存
    private final DistributedCache distributedCache;

    // 幂等性注解，通过SPEL表达式生成唯一键，防止重复提交创建支付请求
    // 唯一键前缀为 "index12306 - pay:lock_create_pay:"，具体键值为请求参数中的outOrderSn
    @Idempotent(type = IdempotentTypeEnum.SPEL, uniqueKeyPrefix = "index12306 - pay:lock_create_pay:", key = "#requestParam.getOutOrderSn()")
    // 声明该方法需要事务管理，遇到任何异常都回滚事务
    @Transactional(rollbackFor = Exception.class)
    // 重写PayService接口中的commonPay方法，实现通用支付功能
    @Override
    public PayRespDTO commonPay(PayRequest requestParam) {
        // 从分布式缓存中获取支付结果信息，缓存键由ORDER_PAY_RESULT_INFO常量和请求参数中的orderSn组成
        PayRespDTO cacheResult = distributedCache.get(ORDER_PAY_RESULT_INFO + requestParam.getOrderSn(),
                PayRespDTO.class);
        // 如果缓存中存在支付结果信息，则直接返回该结果
        if (cacheResult != null) {
            return cacheResult;
        }
        // 通过策略模式，根据请求参数动态选择并执行相应的支付策略，返回支付响应结果
        /**
         * 调用支付策略，根据请求参数动态选择并执行相应的支付策略，返回支付响应结果
         * AliPayNativeHandler 阿里支付组件会执行具体的支付逻辑，包括生成支付二维码、处理支付回调等
         * 最后得到的是一个payPesponse对象，包含了支付结果的相关信息，AliPayNativeHandler的pay方法得到的
         * 从中getBody() 是支付宝返回的原始支付页面HTML内容，返回给前端，前端进入这个html页面就可以重定向到支付宝支付页面
         * 前端需要解析这个html页面，提取出支付二维码，然后展示给用户，用户扫描二维码后即可完成支付
         * 支付完成后支付宝的服务器会异步通知商户支付结果，商户需要实现一个回调URL，用于接收支付宝的支付结果通知
         * 回调URL需要在支付请求中设置，支付宝会将支付结果以POST请求的形式发送到该URL
         * 商户需要在回调URL中解析请求参数，验证签名，确认支付结果是否有效
         * 如果支付结果有效，商户需要更新订单状态为已支付，并返回成功响应给支付宝
         * 如果支付结果无效，商户需要返回失败响应给支付宝
         */
        PayResponse result = abstractStrategyChoose.chooseAndExecuteResp(requestParam.buildMark(), requestParam);
        // 将PayRequest请求参数转换为PayDO对象，用于数据库插入操作
        PayDO insertPay = BeanUtil.convert(requestParam, PayDO.class);
        // 生成支付单号，使用PayIdGeneratorManager工具类根据订单号生成
        String paySn = PayIdGeneratorManager.generateId(requestParam.getOrderSn());
        // 设置PayDO对象的支付单号
        insertPay.setPaySn(paySn);
        // 设置支付单状态为等待买家付款状态
        insertPay.setStatus(TradeStatusEnum.WAIT_BUYER_PAY.tradeCode());
        // 将支付总金额乘以100并进行四舍五入取整，设置到PayDO对象的总金额字段
        insertPay.setTotalAmount(requestParam.getTotalAmount().multiply(new BigDecimal("100"))
                .setScale(0, BigDecimal.ROUND_HALF_UP).intValue());
        // 将PayDO对象插入数据库，返回插入影响的行数
        int insert = payMapper.insert(insertPay);
        // 如果插入影响的行数小于等于0，说明插入失败
        if (insert <= 0) {
            // 记录错误日志，打印支付聚合根信息
            log.error("支付单创建失败，支付聚合根：{}", JSON.toJSONString(requestParam));
            // 抛出业务异常，提示支付单创建失败
            throw new ServiceException("支付单创建失败");
        }
        // 将支付结果信息存入分布式缓存，缓存键由ORDER_PAY_RESULT_INFO常量和请求参数中的orderSn组成
        // 缓存有效期为10分钟
        distributedCache.put(ORDER_PAY_RESULT_INFO + requestParam.getOrderSn(), JSON.toJSONString(result), 10,
                TimeUnit.MINUTES);
        // 将支付响应结果转换为PayRespDTO对象并返回
        return BeanUtil.convert(result, PayRespDTO.class);
    }

    // 声明该方法需要事务管理，遇到任何异常都回滚事务
    @Transactional(rollbackFor = Exception.class)
    // 重写PayService接口中的callbackPay方法，用于处理支付回调
    /*
     * 处理支付回调，更新支付记录并发送支付结果回调订单
     * 前端支付完后，支付宝服务器会发起post请求回调，携带支付结果参数
     * PayCallbackController会接收这个post请求，调用AliPayCallbackHandler处理支付回调
     * AliPayCallbackHandler会解析请求参数，验证签名，确认支付结果是否有效
     * 如果支付结果有效，商户需要更新订单状态为已支付，并返回成功响应给支付宝
     * 更新订单状态就是在AliPayCallbackHandler中调用这个callbackPay方法
     * 如果支付结果无效，商户需要返回失败响应给支付宝
     *
     * @param requestParam 支付回调请求参数，包含订单号、商户订单号、支付渠道、支付环境、订单标题、交易凭证号、交易总金额、付款时间、支付金额、支付状态、商户订单号等信息
     */
    @Override
    public void callbackPay(PayCallbackReqDTO requestParam) {
        // 创建LambdaQueryWrapper对象，用于构建查询条件，查询PayDO表中orderSn等于请求参数中orderSn的记录
        LambdaQueryWrapper<PayDO> queryWrapper = Wrappers.lambdaQuery(PayDO.class)
                .eq(PayDO::getOrderSn, requestParam.getOrderSn());
        // 根据查询条件从数据库中查询一条支付记录
        PayDO payDO = payMapper.selectOne(queryWrapper);
        // 如果查询到的支付记录为null
        if (Objects.isNull(payDO)) {
            // 记录错误日志，打印订单请求ID
            log.error("支付单不存在，orderRequestId：{}", requestParam.getOrderRequestId());
            // 抛出业务异常，提示支付单不存在
            throw new ServiceException("支付单不存在");
        }
        // 设置支付记录的交易号为回调请求参数中的交易号
        payDO.setTradeNo(requestParam.getTradeNo());
        // 设置支付记录的状态为回调请求参数中的状态
        payDO.setStatus(requestParam.getStatus());
        // 设置支付记录的支付金额为回调请求参数中的支付金额
        payDO.setPayAmount(requestParam.getPayAmount());
        // 设置支付记录的支付时间为回调请求参数中的支付时间
        payDO.setGmtPayment(requestParam.getGmtPayment());
        // 创建LambdaUpdateWrapper对象，用于构建更新条件，更新PayDO表中orderSn等于请求参数中orderSn的记录
        LambdaUpdateWrapper<PayDO> updateWrapper = Wrappers.lambdaUpdate(PayDO.class)
                .eq(PayDO::getOrderSn, requestParam.getOrderSn());
        // 根据更新条件和更新后的支付记录，更新数据库中的支付记录，返回更新影响的行数
        int result = payMapper.update(payDO, updateWrapper);
        // 如果更新影响的行数小于等于0，说明更新失败
        if (result <= 0) {
            // 记录错误日志，打印支付单信息
            log.error("修改支付单支付结果失败，支付单信息：{}", JSON.toJSONString(payDO));
            // 抛出业务异常，提示修改支付单支付结果失败
            throw new ServiceException("修改支付单支付结果失败");
        }
        // 如果回调请求参数中的状态等于交易成功状态的交易码
        if (Objects.equals(requestParam.getStatus(), TradeStatusEnum.TRADE_SUCCESS.tradeCode())) {
            // 将支付记录转换为PayResultCallbackOrderEvent对象，并通过生产者发送消息，告知订单服务支付结果
            payResultCallbackOrderSendProduce.sendMessage(BeanUtil.convert(payDO, PayResultCallbackOrderEvent.class));
        }
    }

    // 重写PayService接口中的getPayInfoByOrderSn方法，根据订单号获取支付信息
    @Override
    public PayInfoRespDTO getPayInfoByOrderSn(String orderSn) {
        // 创建LambdaQueryWrapper对象，用于构建查询条件，查询PayDO表中orderSn等于传入订单号的记录
        LambdaQueryWrapper<PayDO> queryWrapper = Wrappers.lambdaQuery(PayDO.class)
                .eq(PayDO::getOrderSn, orderSn);
        // 根据查询条件从数据库中查询一条支付记录
        PayDO payDO = payMapper.selectOne(queryWrapper);
        // 将查询到的支付记录转换为PayInfoRespDTO对象并返回
        return BeanUtil.convert(payDO, PayInfoRespDTO.class);
    }

    // 重写PayService接口中的getPayInfoByPaySn方法，根据支付单号获取支付信息
    @Override
    public PayInfoRespDTO getPayInfoByPaySn(String paySn) {
        // 创建LambdaQueryWrapper对象，用于构建查询条件，查询PayDO表中paySn等于传入支付单号的记录
        LambdaQueryWrapper<PayDO> queryWrapper = Wrappers.lambdaQuery(PayDO.class)
                .eq(PayDO::getPaySn, paySn);
        // 根据查询条件从数据库中查询一条支付记录
        PayDO payDO = payMapper.selectOne(queryWrapper);
        // 将查询到的支付记录转换为PayInfoRespDTO对象并返回
        return BeanUtil.convert(payDO, PayInfoRespDTO.class);
    }

    // 重写PayService接口中的commonRefund方法，实现通用退款功能
    @Override
    public RefundRespDTO commonRefund(RefundReqDTO requestParam) {
        // 创建LambdaQueryWrapper对象，用于构建查询条件，查询PayDO表中orderSn等于请求参数中orderSn的记录
        LambdaQueryWrapper<PayDO> queryWrapper = Wrappers.lambdaQuery(PayDO.class)
                .eq(PayDO::getOrderSn, requestParam.getOrderSn());
        // 根据查询条件从数据库中查询一条支付记录
        PayDO payDO = payMapper.selectOne(queryWrapper);
        // 如果查询到的支付记录为null
        if (Objects.isNull(payDO)) {
            // 记录错误日志，打印订单号
            log.error("支付单不存在，orderSn：{}", requestParam.getOrderSn());
            // 抛出业务异常，提示支付单不存在
            throw new ServiceException("支付单不存在");
        }
        /**
         * {@link AliRefundNativeHandler}
         */
        // 通过策略模式，将支付记录转换为退款命令对象，再转换为退款请求对象
        // 然后动态选择并执行相应的退款策略，返回退款响应结果
        RefundCommand refundCommand = BeanUtil.convert(payDO, RefundCommand.class);
        RefundRequest refundRequest = RefundRequestConvert.command2RefundRequest(refundCommand);
        RefundResponse result = abstractStrategyChoose.chooseAndExecuteResp(refundRequest.buildMark(), refundRequest);
        // 设置支付记录的状态为退款响应结果中的状态
        payDO.setStatus(result.getStatus());
        // 创建LambdaUpdateWrapper对象，用于构建更新条件，更新PayDO表中orderSn等于请求参数中orderSn的记录
        LambdaUpdateWrapper<PayDO> updateWrapper = Wrappers.lambdaUpdate(PayDO.class)
                .eq(PayDO::getOrderSn, requestParam.getOrderSn());
        // 根据更新条件和更新后的支付记录，更新数据库中的支付记录，返回更新影响的行数
        int updateResult = payMapper.update(payDO, updateWrapper);
        // 如果更新影响的行数小于等于0，说明更新失败
        if (updateResult <= 0) {
            // 记录错误日志，打印支付单信息
            log.error("修改支付单退款结果失败，支付单信息：{}", JSON.toJSONString(payDO));
            // 抛出业务异常，提示修改支付单退款结果失败
            throw new ServiceException("修改支付单退款结果失败");
        }
        // 暂时返回null，可能需要后续完善返回逻辑
        return null;
    }
}
