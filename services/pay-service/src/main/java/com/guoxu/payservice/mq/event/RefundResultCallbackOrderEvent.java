package com.guoxu.payservice.mq.event;

import com.guoxu.payservice.common.enums.RefundTypeEnum;
import com.guoxu.payservice.remote.dto.TicketOrderPassengerDetailRespDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * RefundResultCallbackOrderEvent
 * 退款结果回调订单服务事件
 * @author 执笔画棠
 * @date 2025/11/11 18:21
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public final class RefundResultCallbackOrderEvent {
    /**
     * 订单号
     */
    private String orderSn;
    /**
     * 退款类型
     */
    private RefundTypeEnum refundTypeEnum;

    /**
     * 部分退款车票详情
     */
    private List<TicketOrderPassengerDetailRespDTO> partialRefundTicketDetailList;
}
