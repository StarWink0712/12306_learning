package com.guoxu.orderservice.dto.req;

import com.guoxu.page.PageRequest;
import lombok.Data;

/**
 * TicketOrderPageQueryReqDTO
 * 车票订单分页查询
 * @author 执笔画棠
 * @date 2025/11/09 20:46
 **/
@Data
public class TicketOrderPageQueryReqDTO extends PageRequest {

    /**
     * 用户唯一标识
     */
    private String userId;

    /**
     * 状态类型 0：未完成 1：未出行 2：历史订单
     */
    private Integer statusType;
}
