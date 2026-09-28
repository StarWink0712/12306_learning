package com.guoxu.ticketservice.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoxu.base.BaseDo;
import lombok.Data;

/**
 * TrainStationPriceDO 列车站点价格实体
 *
 * @author 执笔画棠
 * @date 2025/11/12 20:38
 **/
@Data
@TableName("t_train_station_price")
public class TrainStationPriceDO extends BaseDo {
    /**
     * id
     */
    private Long id;

    /**
     * 车次id
     */
    private Long trainId;

    /**
     * 座位类型
     */
    private Integer seatType;

    /**
     * 出发站点
     */
    private String departure;

    /**
     * 到达站点
     */
    private String arrival;

    /**
     * 车票价格
     */
    private Integer price;
}
