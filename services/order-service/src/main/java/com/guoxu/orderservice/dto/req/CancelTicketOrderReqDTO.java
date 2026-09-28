package com.guoxu.orderservice.dto.req;

import lombok.Data;

/**
 * CancelTicketOrderReqDTO 关闭火车票订单
 *
 * @author 执笔画棠
 * @date 2025/11/09 20:51
 **/
@Data
public class CancelTicketOrderReqDTO {

    /**
     * 订单号
     */
    private String orderSn;
}
