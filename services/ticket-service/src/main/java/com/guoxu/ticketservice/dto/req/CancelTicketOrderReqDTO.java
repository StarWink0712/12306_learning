package com.guoxu.ticketservice.dto.req;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 取消车票订单请求入参
 *
 * @author 执笔画棠
 * @date 2025/11/12 21:15
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CancelTicketOrderReqDTO {
    /**
     * 订单号
     */
    private String orderSn;
}
