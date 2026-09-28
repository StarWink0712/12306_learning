package com.guoxu.ticketservice.service.handler.ticket.filter.query;

import com.guoxu.exception.ClientException;
import com.guoxu.ticketservice.dto.req.TicketPageQueryReqDTO;
import com.guoxu.ticketservice.service.handler.ticket.filter.purchase.TrainPurchaseTicketChainFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Objects;

/**
 * TrainTicketQueryParamBaseVerifyChainFilter
 * 查询列车车票流程过滤器之基础数据验证
 * @author 执笔画棠
 * @date 2025/11/13 19:55
 **/
@Component
@RequiredArgsConstructor
//意思是这个责任链只处理传入参数是TicketPageQueryReqDTO这一类的http请求
public class TrainTicketQueryParamBaseVerifyChainFilter implements TrainTicketQueryChainFilter<TicketPageQueryReqDTO> {
    @Override
    public void handler(TicketPageQueryReqDTO requestParam) {
        if (requestParam.getDepartureDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
                .isBefore(LocalDate.now())) {
            throw new ClientException("出发日期不能小于当前日期");
        }
        if (Objects.equals(requestParam.getFromStation(), requestParam.getToStation())) {
            throw new ClientException("出发地和目的地不能相同");
        }
    }

    @Override
    public int getOrder() {
        return 10;
    }
}
