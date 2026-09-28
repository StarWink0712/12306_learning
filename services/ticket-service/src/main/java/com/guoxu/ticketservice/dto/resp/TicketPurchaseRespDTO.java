package com.guoxu.ticketservice.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * TicketPurchaseRespDTO
 * 购票响应实体
 * @author 执笔画棠
 * @date 2025/11/12 21:25
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TicketPurchaseRespDTO {
    /**
     * 订单号
     */
    private String orderSn;

    /**
     * 乘车人订单详情
     */
    private List<TicketOrderDetailRespDTO> ticketOrderDetails;
}
