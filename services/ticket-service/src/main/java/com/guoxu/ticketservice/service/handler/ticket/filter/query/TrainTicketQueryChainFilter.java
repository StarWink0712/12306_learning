package com.guoxu.ticketservice.service.handler.ticket.filter.query;

import com.guoxu.chain.AbstractChainHandler;
import com.guoxu.ticketservice.common.enums.TicketChainMarkEnum;
import com.guoxu.ticketservice.dto.req.TicketPageQueryReqDTO;

/**
 * TrainTicketQueryChainFilter 列车车票查询过滤器
 *
 * @author 执笔画棠
 * @version 2025/11/13 19:55
 **/
public interface TrainTicketQueryChainFilter <T extends TicketPageQueryReqDTO>
        extends AbstractChainHandler<TicketPageQueryReqDTO> {
    @Override
    default String mark() {
        return TicketChainMarkEnum.TRAIN_QUERY_FILTER.name();
    }
}