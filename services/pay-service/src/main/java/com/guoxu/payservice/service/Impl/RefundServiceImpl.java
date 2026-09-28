package com.guoxu.payservice.service.Impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.guoxu.exception.ServiceException;
import com.guoxu.payservice.common.enums.TradeStatusEnum;
import com.guoxu.payservice.convent.RefundRequestConvert;
import com.guoxu.payservice.dao.entity.PayDO;
import com.guoxu.payservice.dao.entity.RefundDO;
import com.guoxu.payservice.dao.mapper.PayMapper;
import com.guoxu.payservice.dao.mapper.RefundMapper;
import com.guoxu.payservice.dto.RefundCommand;
import com.guoxu.payservice.dto.RefundCreateDTO;
import com.guoxu.payservice.dto.RefundReqDTO;
import com.guoxu.payservice.dto.RefundRespDTO;
import com.guoxu.payservice.dto.base.RefundRequest;
import com.guoxu.payservice.dto.base.RefundResponse;
import com.guoxu.payservice.mq.event.RefundResultCallbackOrderEvent;
import com.guoxu.payservice.mq.produce.RefundResultCallbackOrderSendProduce;
import com.guoxu.payservice.remote.TicketOrderRemoteService;
import com.guoxu.payservice.remote.dto.TicketOrderDetailRespDTO;
import com.guoxu.payservice.service.RefundService;
import com.guoxu.result.Result;
import com.guoxu.strategy.AbstractStrategyChoose;
import com.guoxu.toolkit.BeanUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Objects;

/**
 * RefundServiceImpl
 * @description: 退款服务实现类
 *
 * @author 执笔画棠
 * @date 2025/11/12 13:55
 **/
@Slf4j
@Service
@RequiredArgsConstructor
public class RefundServiceImpl implements RefundService {

    // 注入支付数据访问对象PayMapper，用于操作支付相关数据库表
    private final PayMapper payMapper;
    // 注入退款数据访问对象RefundMapper，用于操作退款相关数据库表
    private final RefundMapper refundMapper;
    // 注入车票订单远程服务对象TicketOrderRemoteService，用于调用远程车票订单服务
    private final TicketOrderRemoteService ticketOrderRemoteService;
    // 注入抽象策略选择对象AbstractStrategyChoose，用于实现退款策略的动态选择
    private final AbstractStrategyChoose abstractStrategyChoose;
    // 注入退款结果回调订单发送生产者对象RefundResultCallbackOrderSendProduce，用于发送退款结果回调订单的消息
    private final RefundResultCallbackOrderSendProduce refundResultCallbackOrderSendProduce;

    // 重写RefundService接口中的commonRefund方法，实现通用退款功能
    // 使用@Transactional注解，确保该方法内的数据库操作在一个事务中
    @Override
    @Transactional
    public RefundRespDTO commonRefund(RefundReqDTO requestParam) {
        // 定义退款响应数据传输对象refundRespDTO，初始化为null
        RefundRespDTO refundRespDTO = null;
        // 创建一个LambdaQueryWrapper对象queryWrapper，用于构建查询条件，查询PayDO表中orderSn等于请求参数中orderSn的记录
        LambdaQueryWrapper<PayDO> queryWrapper = Wrappers.lambdaQuery(PayDO.class)
                .eq(PayDO::getOrderSn, requestParam.getOrderSn());
        // 使用payMapper根据构建的查询条件从数据库中查询一条支付记录
        PayDO payDO = payMapper.selectOne(queryWrapper);
        // 如果查询到的支付记录为null
        if (Objects.isNull(payDO)) {
            // 记录错误日志，表明支付单不存在，并记录对应的orderSn
            log.error("支付单不存在，orderSn：{}", requestParam.getOrderSn());
            // 抛出业务异常，提示支付单不存在
            throw new ServiceException("支付单不存在");
        }
        // 计算支付单剩余金额，即总金额减去本次退款金额
        payDO.setPayAmount(payDO.getTotalAmount() - requestParam.getRefundAmount());
        // 将请求参数RefundReqDTO转换为RefundCreateDTO对象，用于创建退款单
        RefundCreateDTO refundCreateDTO = BeanUtil.convert(requestParam, RefundCreateDTO.class);
        // 设置退款创建对象中的支付单号，值为查询到的支付记录中的支付单号
        refundCreateDTO.setPaySn(payDO.getPaySn());
        // 调用createRefund方法创建退款单
        createRefund(refundCreateDTO);
        /**
         * {@link AliRefundNativeHandler}
         */
        // 将支付记录PayDO转换为退款命令对象RefundCommand
        RefundCommand refundCommand = BeanUtil.convert(payDO, RefundCommand.class);
        // 设置退款命令对象中的退款金额，值为请求参数中的退款金额
        refundCommand.setPayAmount(new BigDecimal(requestParam.getRefundAmount()));
        // 将退款命令对象转换为退款请求对象RefundRequest
        RefundRequest refundRequest = RefundRequestConvert.command2RefundRequest(refundCommand);
        // 通过策略选择器选择并执行对应的退款策略，返回退款响应结果
        RefundResponse result = abstractStrategyChoose.chooseAndExecuteResp(refundRequest.buildMark(), refundRequest);
        // 设置支付记录的状态为退款响应结果中的状态
        payDO.setStatus(result.getStatus());
        // 创建一个LambdaUpdateWrapper对象updateWrapper，用于构建更新条件，更新PayDO表中orderSn等于请求参数中orderSn的记录
        LambdaUpdateWrapper<PayDO> updateWrapper = Wrappers.lambdaUpdate(PayDO.class)
                .eq(PayDO::getOrderSn, requestParam.getOrderSn());
        // 使用payMapper根据构建的更新条件和更新数据，更新数据库中的支付记录
        int updateResult = payMapper.update(payDO, updateWrapper);
        // 如果更新影响的行数小于等于0
        if (updateResult <= 0) {
            // 记录错误日志，表明修改支付单退款结果失败，并记录支付单信息
            log.error("修改支付单退款结果失败，支付单信息：{}", JSON.toJSONString(payDO));
            // 抛出业务异常，提示修改支付单退款结果失败
            throw new ServiceException("修改支付单退款结果失败");
        }
        // 创建一个LambdaUpdateWrapper对象refundUpdateWrapper，用于构建更新条件，更新RefundDO表中orderSn等于请求参数中orderSn的记录
        LambdaUpdateWrapper<RefundDO> refundUpdateWrapper = Wrappers.lambdaUpdate(RefundDO.class)
                .eq(RefundDO::getOrderSn, requestParam.getOrderSn());
        // 创建一个退款记录对象RefundDO
        RefundDO refundDO = new RefundDO();
        // 设置退款记录对象中的交易号，值为退款响应结果中的交易号
        refundDO.setTradeNo(result.getTradeNo());
        // 设置退款记录对象中的状态，值为退款响应结果中的状态
        refundDO.setStatus(result.getStatus());
        // 使用refundMapper根据构建的更新条件和更新数据，更新数据库中的退款记录
        int refundUpdateResult = refundMapper.update(refundDO, refundUpdateWrapper);
        // 如果更新影响的行数小于等于0
        if (refundUpdateResult <= 0) {
            // 记录错误日志，表明修改退款单退款结果失败，并记录退款单信息
            log.error("修改退款单退款结果失败，退款单信息：{}", JSON.toJSONString(refundDO));
            // 抛出业务异常，提示修改退款单退款结果失败
            throw new ServiceException("修改退款单退款结果失败");
        }
        // 如果退款响应结果的状态等于交易关闭状态的交易码
        if (Objects.equals(result.getStatus(), TradeStatusEnum.TRADE_CLOSED.tradeCode())) {
            // 创建退款结果回调订单事件对象RefundResultCallbackOrderEvent，并设置相关属性
            RefundResultCallbackOrderEvent refundResultCallbackOrderEvent = RefundResultCallbackOrderEvent.builder()
                    .orderSn(requestParam.getOrderSn())
                    .refundTypeEnum(requestParam.getRefundTypeEnum())
                    .partialRefundTicketDetailList(requestParam.getRefundDetailReqDTOList())
                    .build();
            // 使用生产者发送退款结果回调订单事件消息
            refundResultCallbackOrderSendProduce.sendMessage(refundResultCallbackOrderEvent);
        }
        // TODO 暂时返回空实体，后续可能需要完善返回逻辑
        return refundRespDTO;
    }

    // 定义一个私有方法createRefund，用于创建退款单
    private void createRefund(RefundCreateDTO requestParam) {
        // 通过远程服务调用，根据订单号查询车票订单详细信息，返回一个包含查询结果的Result对象
        Result<TicketOrderDetailRespDTO> queryTicketResult = ticketOrderRemoteService
                .queryTicketOrderByOrderSn(requestParam.getOrderSn());
        // 如果查询结果不成功且返回的数据为null
        if (!queryTicketResult.isSuccess() && Objects.isNull(queryTicketResult.getData())) {
            // 抛出业务异常，提示车票订单不存在
            throw new ServiceException("车票订单不存在");
        }
        // 获取查询结果中的车票订单详细信息
        TicketOrderDetailRespDTO orderDetailRespDTO = queryTicketResult.getData();
        // 遍历请求参数中的退款明细列表
        requestParam.getRefundDetailReqDTOList().forEach(each -> {
            // 创建一个退款记录对象RefundDO
            RefundDO refundDO = new RefundDO();
            // 设置退款记录对象中的支付单号，值为请求参数中的支付单号
            refundDO.setPaySn(requestParam.getPaySn());
            // 设置退款记录对象中的订单号，值为请求参数中的订单号
            refundDO.setOrderSn(requestParam.getOrderSn());
            // 设置退款记录对象中的列车ID，值为车票订单详细信息中的列车ID
            refundDO.setTrainId(orderDetailRespDTO.getTrainId());
            // 设置退款记录对象中的列车车次，值为车票订单详细信息中的列车车次
            refundDO.setTrainNumber(orderDetailRespDTO.getTrainNumber());
            // 设置退款记录对象中的出发地，值为车票订单详细信息中的出发地
            refundDO.setDeparture(orderDetailRespDTO.getDeparture());
            // 设置退款记录对象中的目的地，值为车票订单详细信息中的目的地
            refundDO.setArrival(orderDetailRespDTO.getArrival());
            // 设置退款记录对象中的出发时间，值为车票订单详细信息中的出发时间
            refundDO.setDepartureTime(orderDetailRespDTO.getDepartureTime());
            // 设置退款记录对象中的到达时间，值为车票订单详细信息中的到达时间
            refundDO.setArrivalTime(orderDetailRespDTO.getArrivalTime());
            // 设置退款记录对象中的乘车日期，值为车票订单详细信息中的乘车日期
            refundDO.setRidingDate(orderDetailRespDTO.getRidingDate());
            // 设置退款记录对象中的座位类型，值为当前遍历到的退款明细中的座位类型
            refundDO.setSeatType(each.getSeatType());
            // 设置退款记录对象中的证件类型，值为当前遍历到的退款明细中的证件类型
            refundDO.setIdType(each.getIdType());
            // 设置退款记录对象中的身份证号，值为当前遍历到的退款明细中的身份证号
            refundDO.setIdCard(each.getIdCard());
            // 设置退款记录对象中的真实姓名，值为当前遍历到的退款明细中的真实姓名
            refundDO.setRealName(each.getRealName());
            // 设置退款记录对象中的退款时间，值为当前时间
            refundDO.setRefundTime(new Date());
            // 设置退款记录对象中的退款金额，值为当前遍历到的退款明细中的金额
            refundDO.setAmount(each.getAmount());
            // 设置退款记录对象中的用户ID，值为当前遍历到的退款明细中的用户ID
            refundDO.setUserId(each.getUserId());
            // 设置退款记录对象中的用户名，值为当前遍历到的退款明细中的用户名
            refundDO.setUsername(each.getUsername());
            // 使用refundMapper将退款记录插入到数据库中
            refundMapper.insert(refundDO);
        });
    }
}
