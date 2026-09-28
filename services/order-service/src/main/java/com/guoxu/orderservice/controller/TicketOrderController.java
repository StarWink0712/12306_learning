package com.guoxu.orderservice.controller;

import com.guoxu.Results;
import com.guoxu.orderservice.dto.req.*;
import com.guoxu.orderservice.dto.resp.TicketOrderDetailRespDTO;
import com.guoxu.orderservice.dto.resp.TicketOrderDetailSelfRespDTO;
import com.guoxu.orderservice.dto.resp.TicketOrderPassengerDetailRespDTO;
import com.guoxu.orderservice.service.OrderItemService;
import com.guoxu.orderservice.service.OrderService;
import com.guoxu.page.PageResponse;
import lombok.RequiredArgsConstructor;
import org.opengoofy.index12306.framework.starter.convention.result.Result;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * TicketOrderController
 * 车票订单接口控制层
 * @author 执笔画棠
 * @date 2025/11/09 20:26
 **/
@RestController
@RequiredArgsConstructor
public class TicketOrderController {

    private final OrderService orderService;
    private final OrderItemService orderItemService;

    /**
     * 根据订单号查询车票订单
     */
    @GetMapping("/api/order-service/order/ticket/query")
    public Result<TicketOrderDetailRespDTO> queryTicketOrderByOrderSn(@RequestParam(value = "orderSn") String orderSn) {
        //@requestParam注解用于获取请求参数orderSn
        return Results.success(orderService.queryTicketOrderByOrderSn(orderSn));
    }


    /**
     * 根据子订单记录id查询车票子订单详情
     */
    @GetMapping("/api/order-service/order/item/ticket/query")
    public Result<List<TicketOrderPassengerDetailRespDTO>> queryTicketItemOrderById(
            TicketOrderItemQueryReqDTO requestParam) {
        return Results.success(orderItemService.queryTicketItemOrderById(requestParam));
    }

    /**
     * 分页查询车票订单
     */
    @GetMapping("/api/order-service/order/ticket/page")
    public Result<PageResponse<TicketOrderDetailRespDTO>> pageTicketOrder(TicketOrderPageQueryReqDTO requestParam) {
        return Results.success(orderService.pageTicketOrder(requestParam));
    }

    /**
     * 分页查询本人车票订单
     */
    @GetMapping("/api/order-service/order/ticket/self/page")
    public Result<PageResponse<TicketOrderDetailSelfRespDTO>> pageSelfTicketOrder(
            TicketOrderSelfPageQueryReqDTO requestParam) {
        return Results.success(orderService.pageSelfTicketOrder(requestParam));
    }

    /**
     * 车票订单创建
     */
    @PostMapping("/api/order-service/order/ticket/create")
    public Result<String> createTicketOrder(@RequestBody TicketOrderCreateReqDTO requestParam) {
        //@RequestBody注解用于获取请求体中的JSON数据并将其绑定到requestParam参数上
        return Results.success(orderService.createTicketOrder(requestParam));
    }

    /**
     * 车票订单关闭
     */
    @PostMapping("/api/order-service/order/ticket/close")
    public Result<Boolean> closeTickOrder(@RequestBody CancelTicketOrderReqDTO requestParam) {
        return Results.success(orderService.closeTickOrder(requestParam));
    }

    /**
     * 车票订单取消
     */
    @PostMapping("/api/order-service/order/ticket/cancel")
    public Result<Boolean> cancelTickOrder(@RequestBody CancelTicketOrderReqDTO requestParam) {
        return Results.success(orderService.cancelTickOrder(requestParam));
    }



}
