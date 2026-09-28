package com.guoxu.ticketservice.service.handler.ticket.filter.refund;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.guoxu.exception.ClientException;
import com.guoxu.ticketservice.common.enums.RefundTypeEnum;
import com.guoxu.ticketservice.dto.req.RefundTicketReqDTO;
import org.springframework.stereotype.Component;

/**
 * TrainRefundTicketParamNotNullChainFilter
 * 列车车票退款流程过滤器之验证数据是否为空或空的字符串
 * @author 执笔画棠
 * @date 2025/11/13 20:01
 **/
@Component
public class TrainRefundTicketParamNotNullChainFilter implements TrainRefundTicketChainFilter<RefundTicketReqDTO> {
    @Override
    public void handler(RefundTicketReqDTO requestParam) {
        if (StrUtil.isBlank(requestParam.getOrderSn())) {
            throw new ClientException("订单号不能为空");
        }
        if (requestParam.getType() == null) {
            throw new ClientException("退款类型不能为空");
        }
        if (requestParam.getType().equals(RefundTypeEnum.PARTIAL_REFUND.getType())) {
            if (CollUtil.isEmpty(requestParam.getSubOrderRecordIdReqList())) {
                throw new ClientException("部分退款子订单记录集合不能为空");
            }
        }
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
