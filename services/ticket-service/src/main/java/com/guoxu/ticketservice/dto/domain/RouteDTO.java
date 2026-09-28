package com.guoxu.ticketservice.dto.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * RouteDTO
 * 站点路线实体
 * @author 执笔画棠
 * @date 2025/11/12 21:12
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RouteDTO {
    /**
     * 出发站点
     */
    private String startStation;

    /**
     * 目的站点
     */
    private String endStation;
}
