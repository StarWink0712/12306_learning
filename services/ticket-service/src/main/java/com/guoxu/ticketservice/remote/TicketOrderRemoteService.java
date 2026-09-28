package com.guoxu.ticketservice.remote;

import com.guoxu.result.Result;
import com.guoxu.ticketservice.dto.req.CancelTicketOrderReqDTO;
import com.guoxu.ticketservice.dto.req.TicketOrderItemQueryReqDTO;
import com.guoxu.ticketservice.remote.dto.TicketOrderCreateRemoteReqDTO;
import com.guoxu.ticketservice.remote.dto.TicketOrderDetailRespDTO;
import com.guoxu.ticketservice.remote.dto.TicketOrderPassengerDetailRespDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * TicketOrderRemoteService
 * 车票订单远程服务调用
 * @author 执笔画棠
 * @version 2025/11/12 19:28
 **/
/*
    这段代码是一个 Feign 客户端接口，
    它是 Spring Cloud 微服务架构中用于服务间通信的核心组件。
    简单来说，它定义了一个 Java 接口，通过注解的方式，将接口中的方法映射为对远程 HTTP 服务的调用。
 */
@FeignClient(value = "index12306-order${unique-name:}-service", url = "${aggregation.remote-url:}")
public interface TicketOrderRemoteService {
    /**
     * 跟据订单号查询车票订单
     *
     * @param orderSn 列车订单号
     * @return 列车订单记录
     */
    @GetMapping("/api/order-service/order/ticket/query")
    Result<TicketOrderDetailRespDTO> queryTicketOrderByOrderSn(@RequestParam(value = "orderSn") String orderSn);

    /**
     * 跟据子订单记录id查询车票子订单详情
     */
    @GetMapping("/api/order-service/order/item/ticket/query")
    Result<List<TicketOrderPassengerDetailRespDTO>> queryTicketItemOrderById(
            @SpringQueryMap TicketOrderItemQueryReqDTO requestParam);

    /**
     * 创建车票订单
     * 返回的是订单号
     * @param requestParam 创建车票订单请求参数
     * @return 订单号
     */
    @PostMapping("/api/order-service/order/ticket/create")
    Result<String> createTicketOrder(@RequestBody TicketOrderCreateRemoteReqDTO requestParam);

    /**
     * 车票订单关闭
     *
     * @param requestParam 车票订单关闭入参
     * @return 关闭订单返回结果
     */
    @PostMapping("/api/order-service/order/ticket/close")
    Result<Boolean> closeTickOrder(@RequestBody CancelTicketOrderReqDTO requestParam);

    /**
     * 车票订单取消
     *
     * @param requestParam 车票订单取消入参
     * @return 订单取消返回结果
     */
    @PostMapping("/api/order-service/order/ticket/cancel")
    Result<Void> cancelTicketOrder(@RequestBody CancelTicketOrderReqDTO requestParam);
}