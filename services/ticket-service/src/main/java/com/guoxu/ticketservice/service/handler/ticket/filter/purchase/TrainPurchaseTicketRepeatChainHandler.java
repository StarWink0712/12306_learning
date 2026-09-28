package com.guoxu.ticketservice.service.handler.ticket.filter.purchase;

import com.guoxu.ticketservice.dto.req.PurchaseTicketReqDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * TrainPurchaseTicketRepeatChainHandler
 * 购票流程过滤器之验证乘客是否重复购买
 * @author 执笔画棠
 * @date 2025/11/13 19:51
 **/
@Component
@RequiredArgsConstructor
public class TrainPurchaseTicketRepeatChainHandler implements TrainPurchaseTicketChainFilter<PurchaseTicketReqDTO>{

    @Override
    public void handler(PurchaseTicketReqDTO requestParam) {
        // TODO 重复购买验证后续实现
    }

    @Override
    public int getOrder() {
        return 30;
    }
}
