package com.guoxu.ticketservice.service.handler.ticket.filter.refund;

import com.guoxu.chain.AbstractChainHandler;
import com.guoxu.ticketservice.common.enums.TicketChainMarkEnum;
import com.guoxu.ticketservice.dto.req.RefundTicketReqDTO;

/**
 * TrainRefundTicketChainFilter
 * 列车车票退款过滤器
 * @author 执笔画棠
 * @version 2025/11/13 20:01
 **/
public interface TrainRefundTicketChainFilter <T extends RefundTicketReqDTO>
        extends AbstractChainHandler<RefundTicketReqDTO> {
    @Override
    default String mark() {
        return TicketChainMarkEnum.TRAIN_REFUND_TICKET_FILTER.name();
    }
}