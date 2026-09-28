package com.guoxu.ticketservice.service.handler.ticket.filter.query;

import cn.hutool.core.util.StrUtil;
import com.guoxu.exception.ClientException;
import com.guoxu.ticketservice.dto.req.TicketPageQueryReqDTO;
import org.springframework.stereotype.Component;

/**
 * TrainTicketQueryParamNotNullChainFilter
 * 查询列车车票流程过滤器之验证数据是否为空或空的字符串
 * @author 执笔画棠
 * @date 2025/11/13 19:57
 **/
@Component
public class TrainTicketQueryParamNotNullChainFilter implements TrainTicketQueryChainFilter<TicketPageQueryReqDTO> {
    @Override
    public void handler(TicketPageQueryReqDTO requestParam) {
        if (StrUtil.isBlank(requestParam.getFromStation())) {
            throw new ClientException("出发地不能为空");
        }
        if (StrUtil.isBlank(requestParam.getToStation())) {
            throw new ClientException("目的地不能为空");
        }
        if (requestParam.getDepartureDate() == null) {
            throw new ClientException("出发日期不能为空");
        }
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
