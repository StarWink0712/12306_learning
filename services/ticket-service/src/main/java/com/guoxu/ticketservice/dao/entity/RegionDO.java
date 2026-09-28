package com.guoxu.ticketservice.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoxu.base.BaseDo;
import lombok.Data;

/**
 * RegionDO 地区实体
 *
 * @author 执笔画棠
 * @date 2025/11/12 20:31
 **/
@Data
@TableName("t_region")
public class RegionDO extends BaseDo {

    /**
     * id
     */
    private Long id;

    /**
     * 地区名称
     */
    private String name;

    /**
     * 地区全名
     */
    private String fullName;

    /**
     * 地区编码
     */
    private String code;

    /**
     * 地区首字母
     */
    private String initial;

    /**
     * 拼音
     */
    private String spell;

    /**
     * 热门标识
     */
    private Integer popularFlag;
}
