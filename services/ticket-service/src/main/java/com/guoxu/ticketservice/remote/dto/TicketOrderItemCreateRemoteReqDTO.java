package com.guoxu.ticketservice.remote.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * TicketOrderItemCreateRemoteReqDTO
 * 车票订单详情创建请求参数
 * @author 执笔画棠
 * @date 2025/11/12 22:20
 **/
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketOrderItemCreateRemoteReqDTO {
    /**
     * 车厢号
     */
    private String carriageNumber;

    /**
     * 座位类型
     */
    private Integer seatType;

    /**
     * 座位号
     */
    private String seatNumber;

    /**
     * 真实姓名
     */
    private String realName;

    /**
     * 证件类型
     */
    private Integer idType;

    /**
     * 证件号
     */
    private String idCard;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 订单金额
     */
    private Integer amount;

    /**
     * 车票类型
     */
    private Integer ticketType;
}
