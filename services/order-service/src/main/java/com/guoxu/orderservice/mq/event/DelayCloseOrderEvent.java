package com.guoxu.orderservice.mq.event;

import com.guoxu.orderservice.dto.req.TicketOrderItemCreateReqDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DelayCloseOrderEvent 订单延迟关闭事件
 * 用于在订单创建后延迟关闭订单，确保用户有足够时间完成支付
 * @author 执笔画棠
 * @date 2025/11/10 20:01
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DelayCloseOrderEvent {
    /**
     * 车次 ID
     */
    private String trainId;

    /**
     * 出发站点
     */
    private String departure;

    /**
     * 到达站点
     */
    private String arrival;

    /**
     * 订单号
     */
    private String orderSn;

    /**
     * 乘车人购票信息
     */
    private List<TicketOrderItemCreateReqDTO> trainPurchaseTicketResults;
}
