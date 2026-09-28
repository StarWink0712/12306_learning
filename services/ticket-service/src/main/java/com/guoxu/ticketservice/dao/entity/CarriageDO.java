package com.guoxu.ticketservice.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.guoxu.base.BaseDo;
import lombok.Data;

/**
 * CarriageDO
 * 车厢实体类
 * @author 执笔画棠
 * @date 2025/11/12 20:31
 **/
@Data
@TableName("t_carriage")
public class CarriageDO extends BaseDo {
    /**
     * id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 列车id
     */
    private Long trainId;

    /**
     * 车厢号
     */
    private String carriageNumber;

    /**
     * 车厢类型
     */
    private Integer carriageType;

    /**
     * 座位数
     */
    private Integer seatCount;
}
