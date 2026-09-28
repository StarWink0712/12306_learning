package com.guoxu.orderservice.dto.req;

import lombok.Data;

import java.util.List;

/**
 * TicketOrderItemQueryReqDTO
 * 车票订单子查询
 * @author 执笔画棠
 * @date 2025/11/09 21:11
 **/
@Data
public class TicketOrderItemQueryReqDTO {

    /**
     * 订单号
     */
    private String orderSn;

    /**
     * 子订单记录id
     */
    private List<Long> orderItemRecordIds;
}
