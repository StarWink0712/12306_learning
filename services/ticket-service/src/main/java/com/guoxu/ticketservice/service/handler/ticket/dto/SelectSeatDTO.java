package com.guoxu.ticketservice.service.handler.ticket.dto;

import com.guoxu.ticketservice.dto.domain.PurchaseTicketPassengerDetailDTO;
import com.guoxu.ticketservice.dto.req.PurchaseTicketReqDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * SelectSeatDTO
 * 选择座位实体
 * @author 执笔画棠
 * @date 2025/11/13 19:37
 **/
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public abstract class SelectSeatDTO {
    /**
     * 座位类型
     */
    private Integer seatType;

    /**
     * 座位对应的乘车人集合
     */
    private List<PurchaseTicketPassengerDetailDTO> passengerSeatDetails;

    /**
     * 购票原始入参
     */
    private PurchaseTicketReqDTO requestParam;
}
