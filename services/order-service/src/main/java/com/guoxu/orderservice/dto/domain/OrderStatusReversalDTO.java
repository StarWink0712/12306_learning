package com.guoxu.orderservice.dto.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * OrderStatusReversalDTO
 *
 * @author 执笔画棠
 * @date 2025/11/09 20:53
 **/
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public final class OrderStatusReversalDTO {
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
     * 订单明细就是订单中的每一个订单项
     */
    private Integer orderItemStatus;
}
