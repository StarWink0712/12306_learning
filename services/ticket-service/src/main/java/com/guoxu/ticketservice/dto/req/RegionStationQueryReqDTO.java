package com.guoxu.ticketservice.dto.req;

import lombok.Data;

/**
 * RegionStationQueryReqDTO
 * 地区&站点查询请求入参
 * @author 执笔画棠
 * @date 2025/11/12 21:17
 **/
@Data
public class RegionStationQueryReqDTO {
    /**
     * 查询方式
     */
    private Integer queryType;

    /**
     * 名称
     */
    private String name;
}
