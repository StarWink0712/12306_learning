package com.guoxu.payservice.remote;

import com.guoxu.orderservice.dto.resp.TicketOrderDetailRespDTO;
import com.guoxu.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * TicketOrderRemoteService
 * 车票订单远程服务调用
 * @author 执笔画棠
 * @date 2025/11/11 14:51
 **/
//这个注解是用来声明一个Feign客户端的，value属性指定了客户端的名称，url属性指定了服务的URL
/*
 * Feign 是一个声明式的、模板化的 HTTP 客户端，
 * 它的主要目标是让编写 Java HTTP 客户端变得更容易、更优雅，尤其是在微服务架构中调用其他服务时。
 * 你可以把它理解为 “服务间调用的快递员”。
 */
@FeignClient(value = "index12306-order${unique-name:}-service", url = "${aggregation.remote-url:}")
public interface TicketOrderRemoteService {
    /**
     * 跟据订单号查询车票订单
     * 通过远程服务调用，根据订单号查询车票订单详细信息，返回一个包含查询结果的Result对象
     * @param orderSn 列车订单号
     * @return 列车订单记录
     */
    @GetMapping("/api/order-service/order/ticket/query")
    Result<TicketOrderDetailRespDTO> queryTicketOrderByOrderSn(@RequestParam(value = "orderSn") String orderSn);
}
