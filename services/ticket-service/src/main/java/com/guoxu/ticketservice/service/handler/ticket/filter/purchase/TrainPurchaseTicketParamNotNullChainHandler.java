package com.guoxu.ticketservice.service.handler.ticket.filter.purchase;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.guoxu.exception.ClientException;
import com.guoxu.ticketservice.dto.domain.PurchaseTicketPassengerDetailDTO;
import com.guoxu.ticketservice.dto.req.PurchaseTicketReqDTO;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * TrainPurchaseTicketParamNotNullChainHandler
 * 购票流程过滤器之验证参数必填
 * @author 执笔画棠
 * @date 2025/11/13 19:45
 **/
@Component
public class TrainPurchaseTicketParamNotNullChainHandler
    implements TrainPurchaseTicketChainFilter<PurchaseTicketReqDTO> {
    @Override
    public void handler(PurchaseTicketReqDTO requestParam) {
        if (StrUtil.isBlank(requestParam.getTrainId())) {
            throw new ClientException("列车标识不能为空");
        }
        if (StrUtil.isBlank(requestParam.getDeparture())) {
            throw new ClientException("出发站点不能为空");
        }
        if (StrUtil.isBlank(requestParam.getArrival())) {
            throw new ClientException("到达站点不能为空");
        }
        if (CollUtil.isEmpty(requestParam.getPassengers())) {
            throw new ClientException("乘车人至少选择一位");
        }
        for (PurchaseTicketPassengerDetailDTO each : requestParam.getPassengers()) {
            if (StrUtil.isBlank(each.getPassengerId())) {
                throw new ClientException("乘车人不能为空");
            }
            if (Objects.isNull(each.getSeatType())) {
                throw new ClientException("座位类型不能为空");
            }
        }
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
