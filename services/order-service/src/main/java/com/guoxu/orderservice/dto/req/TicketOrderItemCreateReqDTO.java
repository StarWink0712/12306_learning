package com.guoxu.orderservice.dto.req;

import lombok.Data;

/**
 * TicketOrderItemCreateReqDTO
 * 票务订单项创建请求DTO
 * @author 执笔画棠
 * @date 2025/11/09 20:50
 **/
@Data
public class TicketOrderItemCreateReqDTO {

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
     * 乘车人 ID
     */
    private String passengerId;

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
