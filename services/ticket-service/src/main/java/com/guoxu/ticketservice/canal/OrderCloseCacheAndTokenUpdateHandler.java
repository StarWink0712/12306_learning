package com.guoxu.ticketservice.canal;

import cn.hutool.core.collection.CollUtil;
import com.guoxu.result.Result;
import com.guoxu.strategy.AbstractExecuteStrategy;

import com.guoxu.ticketservice.common.enums.CanalExecuteStrategyMarkEnum;
import com.guoxu.ticketservice.mq.event.CanalBinlogEvent;
import com.guoxu.ticketservice.remote.TicketOrderRemoteService;
import com.guoxu.ticketservice.remote.dto.TicketOrderDetailRespDTO;
import com.guoxu.ticketservice.remote.dto.TicketOrderPassengerDetailRespDTO;
import com.guoxu.ticketservice.service.SeatService;
import com.guoxu.ticketservice.service.handler.ticket.dto.TrainPurchaseTicketRespDTO;
import com.guoxu.ticketservice.service.handler.ticket.tokenbucket.TicketAvailabilityTokenBucket;
import com.guoxu.toolkit.BeanUtil;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * OrderCloseCacheAndTokenUpdateHandler
 * 用于处理订单关闭时的缓存和令牌更新操作
 * OrderCloseCacheAndTokenUpdateHandler类实现了AbstractExecuteStrategy接口，
 * @author 执笔画棠
 * @date 2025/11/13 22:08
 **/
public class OrderCloseCacheAndTokenUpdateHandler implements AbstractExecuteStrategy<CanalBinlogEvent, Void> {

    // 注入车票订单远程服务对象，用于查询车票订单信息
    private final TicketOrderRemoteService ticketOrderRemoteService;
    // 注入座位服务对象，用于解锁座位相关操作
    private final SeatService seatService;
    // 注入车票可用性令牌桶对象，用于处理车票可用性的令牌操作
    private final TicketAvailabilityTokenBucket ticketAvailabilityTokenBucket;

    // 执行具体操作的方法，处理接收到的CanalBinlogEvent事件
    @Override
    public void execute(CanalBinlogEvent message) {
        // 从事件数据中过滤出状态不为空且状态值为"30"的记录，并转换为列表
        List<Map<String, Object>> messageDataList = message.getData().stream()
                .filter(each -> each.get("status") != null)
                .filter(each -> Objects.equals(each.get("status"), "30"))
                .toList();
        // 如果过滤后的列表为空，直接返回，不进行后续操作
        if (CollUtil.isEmpty(messageDataList)) {
            return;
        }
        // 遍历过滤后的消息数据列表
        for (Map<String, Object> each : messageDataList) {
            // 根据订单号查询车票订单详细信息
            Result<TicketOrderDetailRespDTO> orderDetailResult = ticketOrderRemoteService
                    .queryTicketOrderByOrderSn(each.get("order_sn").toString());
            // 获取查询结果中的车票订单详细数据
            TicketOrderDetailRespDTO orderDetailResultData = orderDetailResult.getData();
            // 如果查询成功且数据不为空
            if (orderDetailResult.isSuccess() && orderDetailResultData != null) {
                // 获取列车ID并转换为字符串
                String trainId = String.valueOf(orderDetailResultData.getTrainId());
                // 获取订单乘客详细信息列表
                List<TicketOrderPassengerDetailRespDTO> passengerDetails = orderDetailResultData.getPassengerDetails();
                // 调用座位服务的解锁方法，解锁相关座位
                seatService.unlock(trainId, orderDetailResultData.getDeparture(), orderDetailResultData.getArrival(),
                        BeanUtil.convert(passengerDetails, TrainPurchaseTicketRespDTO.class));
                // 调用车票可用性令牌桶的回滚方法，将订单相关的车票令牌回滚到桶中
                ticketAvailabilityTokenBucket.rollbackInBucket(orderDetailResultData);
            }
        }
    }

    // 返回标记，用于标识该策略对应的实际表名
    @Override
    public String mark() {
        return CanalExecuteStrategyMarkEnum.T_ORDER.getActualTable();
    }

    // 返回模式匹配标记，用于标识该策略对应的模式匹配表名
    @Override
    public String patternMatchMark() {
        return CanalExecuteStrategyMarkEnum.T_ORDER.getPatternMatchTable();
    }
}
