package com.guoxu.ticketservice.dto.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * SeatTypeCountDTO
 * 座位类型和座位数量实体
 * @author 执笔画棠
 * @date 2025/11/12 21:13
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SeatTypeCountDTO {
    /**
     * 座位类型
     */
    private Integer seatType;

    /**
     * 座位类型 - 对应数量
     */
    private Integer seatCount;
}
