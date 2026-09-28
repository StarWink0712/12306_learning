package com.guoxu.ticketservice.controller;

import com.guoxu.Results;
import com.guoxu.result.Result;
import com.guoxu.ticketservice.dto.req.CancelTicketOrderReqDTO;
import com.guoxu.ticketservice.dto.req.PurchaseTicketReqDTO;
import com.guoxu.ticketservice.dto.req.RefundTicketReqDTO;
import com.guoxu.ticketservice.dto.req.TicketPageQueryReqDTO;
import com.guoxu.ticketservice.dto.resp.RefundTicketRespDTO;
import com.guoxu.ticketservice.dto.resp.TicketPageQueryRespDTO;
import com.guoxu.ticketservice.dto.resp.TicketPurchaseRespDTO;
import com.guoxu.ticketservice.remote.dto.PayInfoRespDTO;
import com.guoxu.ticketservice.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * TicketController
 * 车票控制层
 * @author 执笔画棠
 * @date 2025/11/12 20:22
 **/
@RestController
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    /**
     * 根据条件查询车票
     */
    @GetMapping("/api/ticket-service/ticket/query")
    public Result<TicketPageQueryRespDTO> pageListTicketQuery(TicketPageQueryReqDTO requestParam) {
        return Results.success(ticketService.pageListTicketQueryV1(requestParam));
    }

    /**
     * 购买车票
     */
    @PostMapping("/api/ticket-service/ticket/purchase")
    public Result<TicketPurchaseRespDTO> purchaseTickets(@RequestBody PurchaseTicketReqDTO requestParam) {
        return Results.success(ticketService.purchaseTicketsV1(requestParam));
    }

    /**
     * 购买车票v2
     */
    @PostMapping("/api/ticket-service/ticket/purchase/v2")
    public Result<TicketPurchaseRespDTO> purchaseTicketsV2(@RequestBody PurchaseTicketReqDTO requestParam) {
        return Results.success(ticketService.purchaseTicketsV2(requestParam));
    }

    /**
     * 取消车票订单
     */
    @PostMapping("/api/ticket-service/ticket/cancel")
    public Result<Void> cancelTicketOrder(@RequestBody CancelTicketOrderReqDTO requestParam) {
        ticketService.cancelTicketOrder(requestParam);
        return Results.success();
    }

    /**
     * 支付单详情查询
     */
    @GetMapping("/api/ticket-service/ticket/pay/query")
    public Result<PayInfoRespDTO> getPayInfo(@RequestParam(value = "orderSn") String orderSn) {
        return Results.success(ticketService.getPayInfo(orderSn));
    }

    /**
     * 公共退款接口
     */
    @PostMapping("/api/ticket-service/ticket/refund")
    public Result<RefundTicketRespDTO> commonTicketRefund(@RequestBody RefundTicketReqDTO requestParam) {
        return Results.success(ticketService.commonTicketRefund(requestParam));
    }
}
