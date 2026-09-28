package com.guoxu.ticketservice.dto.req;

import lombok.Data;

import java.util.List;

/**
 * TicketOrderItemQueryReqDTO
 * 车票子订单查询
 * @author 执笔画棠
 * @date 2025/11/12 21:17
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
    private List<String> orderItemRecordIds;
}
