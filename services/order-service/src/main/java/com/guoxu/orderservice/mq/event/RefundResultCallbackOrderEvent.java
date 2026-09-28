package com.guoxu.orderservice.mq.event;

import com.guoxu.orderservice.common.enums.RefundTypeEnum;
import com.guoxu.orderservice.dto.resp.TicketOrderPassengerDetailRespDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.util.List;

/**
 * RefundResultCallbackOrderEvent
 * 退款结果回调订单服务事件
 * @author 执笔画棠
 * @date 2025/11/10 19:38
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
public final class RefundResultCallbackOrderEvent {
    /**
     * 订单号
     */
    private String orderSn;

    /**
     * 退款类型枚举
     */
    private RefundTypeEnum refundTypeEnum;

    /**
     * 部分退款车票详情
     */
    private List<TicketOrderPassengerDetailRespDTO> partialRefundTicketDetailList;
}
