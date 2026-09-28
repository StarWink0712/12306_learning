package com.guoxu.orderservice.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoxu.base.BaseDo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * OrderDO
 * 订单数据库实体
 * @author 执笔画棠
 * @date 2025/11/10 20:21
 **/
@Data
@TableName("t_order")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderDO extends BaseDo {

    /**
     * id
     */
    private Long id;

    /**
     * 订单号
     */
    private String orderSn;

    /**
     * 用户id
     */
    private String userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 列车id
     */
    private Long trainId;

    /**
     * 列车车次
     */
    private String trainNumber;

    /**
     * 出发站点
     */
    private String departure;

    /**
     * 到达站点
     */
    private String arrival;

    /**
     * 订单来源
     */
    private Integer source;

    /**
     * 订单状态
     */
    private Integer status;

    /**
     * 下单时间
     */
    private Date orderTime;

    /**
     * 支付方式
     */
    private Integer payType;

    /**
     * 支付时间
     */
    private Date payTime;

    /**
     * 乘车日期
     */
    private Date ridingDate;

    /**
     * 出发时间
     */
    private Date departureTime;

    /**
     * 出发时间
     */
    private Date arrivalTime;
}
