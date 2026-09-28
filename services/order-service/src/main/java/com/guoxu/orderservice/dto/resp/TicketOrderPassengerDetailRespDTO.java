package com.guoxu.orderservice.dto.resp;

import cn.crane4j.annotation.AssembleEnum;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.guoxu.orderservice.common.enums.OrderItemStatusEnum;
import com.guoxu.orderservice.serialize.IdCardDesensitizationSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * TicketOrderPassengerDetailRespDTO
 * 乘车人订单详情返回参数，在TicketOrderDetailRespDTO
 * 车票订单详情中，一起返回给前端
 * @author 执笔画棠
 * @date 2025/11/09 20:31
 **/
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketOrderPassengerDetailRespDTO {
    /**
     * ID
     */
    private String id;

    /**
     * 用户id
     */
    private String userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 席别类型
     */
    private Integer seatType;

    /**
     * 车厢号
     */
    private String carriageNumber;

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
     * 注解是为这个字段指定一个自定义的序列化器IdCardDesensitizationSerializer.class
     * 这个序列化器会在将idCard字段转换为JSON字符串时，对身份证号进行脱敏处理
     */
    @JsonSerialize(using = IdCardDesensitizationSerializer.class)
    private String idCard;

    /**
     * 车票类型 0：成人 1：儿童 2：学生 3：残疾军人
     */
    private Integer ticketType;

    /**
     * 订单金额
     */
    private Integer amount;

    /**
     * 车票状态
     * AssembleEnum的核心功能是将枚举（Enum）作为数据源，实现字段的自动填充
     * 根据status字段的值，自动填充statusName字段
     */
    @AssembleEnum(type = OrderItemStatusEnum.class, ref = "statusName")
    private Integer status;

    /**
     * 车票状态名称
     */
    private String statusName;
}
