package com.guoxu.ticketservice.service.handler.ticket.filter.purchase;

import com.guoxu.chain.AbstractChainHandler;
import com.guoxu.ticketservice.common.enums.TicketChainMarkEnum;
import com.guoxu.ticketservice.dto.req.PurchaseTicketReqDTO;

/**
 * TrainPurchaseTicketChainFilter 列车购买车票过滤器
 *
 * @author 执笔画棠
 * @version 2025/11/13 19:44
 **/
public interface TrainPurchaseTicketChainFilter <T extends PurchaseTicketReqDTO>
        extends AbstractChainHandler<PurchaseTicketReqDTO> {
    @Override
    default String mark() {
        return TicketChainMarkEnum.TRAIN_PURCHASE_TICKET_FILTER.name();
    }
}