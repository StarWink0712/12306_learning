package com.guoxu.ticketservice.mq.event;

import com.guoxu.ticketservice.service.handler.ticket.dto.TrainPurchaseTicketRespDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DelayCloseOrderEvent
 *
 * @author 执笔画棠
 * @date 2025/11/12 22:12
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
    private List<TrainPurchaseTicketRespDTO> trainPurchaseTicketResults;
}
