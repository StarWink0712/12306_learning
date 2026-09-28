package com.guoxu.ticketservice.dto.domain;

import lombok.Data;

/**
 * PurchaseTicketPassengerDetailDTO
 * 购票乘车人详情实体
 * @author 执笔画棠
 * @date 2025/11/12 21:10
 **/
@Data
public class PurchaseTicketPassengerDetailDTO {
    /**
     * 乘车人 ID
     */
    private String passengerId;

    /**
     * 座位类型
     */
    private Integer seatType;
}
