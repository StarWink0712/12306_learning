package com.guoxu.ticketservice.common.enums;

/**
 * TicketChainMarkEnum
 * 购票相关责任链 Mark 枚举
 * @author 执笔画棠
 * @version 2025/11/12 20:12
 **/
public enum TicketChainMarkEnum {
    /**
     * 车票查询过滤器
     */
    TRAIN_QUERY_FILTER,

    /**
     * 车票购买过滤器
     */
    TRAIN_PURCHASE_TICKET_FILTER,

    /**
     * 车票退款过滤器
     */
    TRAIN_REFUND_TICKET_FILTER
}