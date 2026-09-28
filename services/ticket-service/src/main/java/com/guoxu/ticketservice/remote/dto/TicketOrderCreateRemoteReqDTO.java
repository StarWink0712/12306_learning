package com.guoxu.ticketservice.remote.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * TicketOrderCreateRemoteReqDTO
 * 车票订单创建请求参数
 * @author 执笔画棠
 * @date 2025/11/12 22:19
 **/
@Data
@Builder
public class TicketOrderCreateRemoteReqDTO {
    /**
     * 用户 ID
     */
    private String userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 车次 ID
     */
    private Long trainId;

    /**
     * 出发站点
     */
    private String departure;

    /**
     * 到达站点
     */
    private String arrival;

    /**
     * 订单来源
     */
    private Integer source;

    /**
     * 下单时间
     */
    private Date orderTime;

    /**
     * 乘车日期
     */
    private Date ridingDate;

    /**
     * 列车车次
     */
    private String trainNumber;

    /**
     * 出发时间
     */
    private Date departureTime;

    /**
     * 到达时间
     */
    private Date arrivalTime;

    /**
     * 订单明细
     */
    private List<TicketOrderItemCreateRemoteReqDTO> ticketOrderItems;
}
