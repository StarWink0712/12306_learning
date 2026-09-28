package com.guoxu.ticketservice.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoxu.base.BaseDo;
import lombok.Data;

import java.util.Date;

/**
 * TrainStationRelationDO
 * 列车站点关系实体
 * @author 执笔画棠
 * @date 2025/11/12 20:39
 **/
@Data
@TableName("t_train_station_relation")
public class TrainStationRelationDO extends BaseDo {
    /**
     * id
     */
    private Long id;

    /**
     * 车次id
     */
    private Long trainId;

    /**
     * 出发站点
     */
    private String departure;

    /**
     * 到达站点
     */
    private String arrival;

    /**
     * 起始城市
     */
    private String startRegion;

    /**
     * 终点城市
     */
    private String endRegion;

    /**
     * 始发站标识
     */
    private Boolean departureFlag;

    /**
     * 终点站标识
     */
    private Boolean arrivalFlag;

    /**
     * 出发时间
     */
    private Date departureTime;

    /**
     * 到达时间
     */
    private Date arrivalTime;
}
