package com.guoxu.ticketservice.dto.resp;

import lombok.Data;

import javax.annotation.security.DenyAll;

/**
 * StationQueryRespDTO
 * 站点分页查询响应参数
 * @author 执笔画棠
 * @date 2025/11/12 21:24
 **/
@Data
public class StationQueryRespDTO {
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

    /**
     * 城市名称
     */
    private String regionName;
}
