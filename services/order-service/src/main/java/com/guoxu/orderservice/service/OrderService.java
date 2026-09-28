package com.guoxu.orderservice.service;

import com.guoxu.orderservice.dto.domain.OrderStatusReversalDTO;
import com.guoxu.orderservice.dto.req.CancelTicketOrderReqDTO;
import com.guoxu.orderservice.dto.req.TicketOrderCreateReqDTO;
import com.guoxu.orderservice.dto.req.TicketOrderPageQueryReqDTO;
import com.guoxu.orderservice.dto.req.TicketOrderSelfPageQueryReqDTO;
import com.guoxu.orderservice.dto.resp.TicketOrderDetailRespDTO;
import com.guoxu.orderservice.dto.resp.TicketOrderDetailSelfRespDTO;
import com.guoxu.orderservice.mq.event.PayResultCallbackOrderEvent;
import com.guoxu.page.PageResponse;

/**
 * OrderService
 *
 * @author 执笔画棠
 * @version 2025/11/09 20:27
 **/
public interface OrderService {

    /**
     * 跟据订单号查询车票订单
     *
     * @param orderSn 订单号
     * @return 订单详情
     */
    TicketOrderDetailRespDTO queryTicketOrderByOrderSn(String orderSn);

    /**
     * 跟据用户名分页查询车票订单
     * PageResponse也是自定义的，返回的就是用户的订单分页数据
     * 前端传过来的是用户标识和订单类型
     * 和当前页和每页显示的数量
     * @param requestParam 跟据用户 ID 分页查询对象
     * @return 订单分页详情
     */
    PageResponse<TicketOrderDetailRespDTO> pageTicketOrder(TicketOrderPageQueryReqDTO requestParam);

    /**
     * 创建火车票订单
     * 这个和第一个的那个订单dto不一样，这个是创建订单
     * 第二个是查询订单，比如创建订单时还没有订单号
     * @param requestParam 商品订单入参
     * @return 订单号
     */
    String createTicketOrder(TicketOrderCreateReqDTO requestParam);


    /**
     * 关闭火车票订单
     * 关闭订单时需要根据订单号来关闭
     * 后端返回布尔型表示是否成功关闭订单
     * @param requestParam 关闭火车票订单入参
     */
    boolean closeTickOrder(CancelTicketOrderReqDTO requestParam);


    /**
     * 取消火车票订单
     * 和关闭订单一样
     * @param requestParam 取消火车票订单入参
     */
    boolean cancelTickOrder(CancelTicketOrderReqDTO requestParam);

    /**
     * 订单状态反转
     * 先todo，后面看接口是如何实现的
     * @param requestParam 请求参数
     */
    void statusReversal(OrderStatusReversalDTO requestParam);

    /**
     * 支付结果回调订单
     * 这个方法的功能先todo，后面看接口是如何实现的
     * @param requestParam 请求参数
     */
    void payCallbackOrder(PayResultCallbackOrderEvent requestParam);

    /**
     * 查询本人车票订单
     * 前端传过来的是分页查询对象
     * 包括当前页和每页显示的数量
     * @param requestParam 请求参数
     * @return 本人车票订单集合
     */
    PageResponse<TicketOrderDetailSelfRespDTO> pageSelfTicketOrder(TicketOrderSelfPageQueryReqDTO requestParam);
}