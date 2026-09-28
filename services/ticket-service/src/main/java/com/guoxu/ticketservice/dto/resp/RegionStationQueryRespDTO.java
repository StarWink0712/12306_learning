package com.guoxu.ticketservice.dto.resp;

import lombok.Data;

/**
 * RegionStationQueryRespDTO
 * 地区&站点分页查询响应参数
 * @author 执笔画棠
 * @date 2025/11/12 21:23
 **/
@Data
public class RegionStationQueryRespDTO {
    /**
     * 名称
     */
    private String name;

    /**
     * 地区编码
     */
    private String code;

    /**
     * 拼音
     */
    private String spell;
}
