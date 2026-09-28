package com.guoxu.orderservice.dto.domain;

import com.guoxu.orderservice.dao.entity.OrderItemDO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * OrderItemStatusReversalDTO
 * 子订单状态反转实体
 * @author 执笔画棠
 * @date 2025/11/09 21:11
 **/
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderItemStatusReversalDTO {

    /**
     * 订单号
     */
    private String orderSn;

    /**
     * 订单反转后状态
     */
    private Integer orderStatus;

    /**
     * 订单明细反转后状态
     */
    private Integer orderItemStatus;

    /**
     * 订单明细集合
     */
    private List<OrderItemDO> orderItemDOList;
}
