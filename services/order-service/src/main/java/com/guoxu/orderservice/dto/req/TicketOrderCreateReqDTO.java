package com.guoxu.orderservice.dto.req;

import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * TicketOrderCreateReqDTO
 * 车票订单创建请求参数
 * @author 执笔画棠
 * @date 2025/11/09 20:49
 **/
@Data
public class TicketOrderCreateReqDTO {


    /**
     * 用户 ID
     */
    private Long userId;

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
     * 更详细的信息，比如每个订单项的座位号、乘车人等
     */
    private List<TicketOrderItemCreateReqDTO> ticketOrderItems;
}
