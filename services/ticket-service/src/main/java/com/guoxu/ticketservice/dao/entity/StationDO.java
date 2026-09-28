package com.guoxu.ticketservice.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoxu.base.BaseDo;
import lombok.Data;

import javax.annotation.security.DenyAll;

/**
 * StationDO 车站实体
 *
 * @author 执笔画棠
 * @date 2025/11/12 20:33
 **/
@Data
@TableName("t_station")
public class StationDO extends BaseDo {
    /**
     * id
     */
    private Long id;

    /**
     * 车站编码
     */
    private String code;

    /**
     * 车站名称
     */
    private String name;

    /**
     * 拼音
     */
    private String spell;

    /**
     * 地区编号
     */
    private String region;

    /**
     * 地区名称
     */
    private String regionName;
}
