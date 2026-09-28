package com.guoxu.orderservice.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.guoxu.orderservice.dao.entity.OrderItemDO;
import com.guoxu.orderservice.dto.domain.OrderItemStatusReversalDTO;
import com.guoxu.orderservice.dto.req.TicketOrderItemQueryReqDTO;
import com.guoxu.orderservice.dto.resp.TicketOrderPassengerDetailRespDTO;

import java.util.List;

/**
 * OrderItemService
 * 订单明细接口层
 * @author 执笔画棠
 * @version 2025/11/09 21:05
 **/
public interface OrderItemService extends IService<OrderItemDO> {

    /**
     * 子订单状态反转
     *
     * @param requestParam 请求参数
     */
    void orderItemStatusReversal(OrderItemStatusReversalDTO requestParam);

    /**
     * 根据子订单记录id查询车票子订单详情
     *
     * @param requestParam 请求参数
     */
    List<TicketOrderPassengerDetailRespDTO> queryTicketItemOrderById(TicketOrderItemQueryReqDTO requestParam);
}