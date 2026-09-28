package com.guoxu.ticketservice.dto.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * SeatClassDTO
 * 席别类型实体
 * @author 执笔画棠
 * @date 2025/11/12 21:13
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SeatClassDTO {
    /**
     * 席别类型
     */
    private Integer type;

    /**
     * 席别数量
     */
    private Integer quantity;

    /**
     * 席别价格
     */
    private BigDecimal price;

    /**
     * 席别候补标识
     */
    private Boolean candidate;
}
