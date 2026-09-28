package com.guoxu.orderservice.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoxu.base.BaseDo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * OrderItemPassengerDO 乘车人订单关系实体
 * “订单与乘车人”之间的多对多关系的实体类
 * @author 执笔画棠
 * @date 2025/11/09 21:20
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@TableName("t_order_item_passenger")
public class OrderItemPassengerDO extends BaseDo {
    /**
     * id
     */
    private Long id;

    /**
     * 订单号
     */
    private String orderSn;

    /**
     * 证件类型
     */
    private Integer idType;

    /**
     * 证件号
     */
    private String idCard;
}
