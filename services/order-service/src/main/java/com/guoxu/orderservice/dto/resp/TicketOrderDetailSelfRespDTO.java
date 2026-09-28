package com.guoxu.orderservice.dto.resp;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * TicketOrderDetailSelfRespDTO
 * 本人车票订单详情返回参数
 * @author 执笔画棠
 * @date 2025/11/09 20:56
 **/
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TicketOrderDetailSelfRespDTO   {
    /**
     * 出发站点
     */
    private String departure;

    /**
     * 到达站点
     */
    private String arrival;

    /**
     * 乘车日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private Date ridingDate;

    /**
     * 列车车次
     */
    private String trainNumber;

    /**
     * 出发时间
     */
    @JsonFormat(pattern = "HH:mm", timezone = "GMT+8")
    private Date departureTime;

    /**
     * 到达时间
     * 这是一个功能强大的注解，用于在序列化（对象转JSON）和反序列化（JSON转对象）时，为特定的属性值指定格式。[
     * 例如，将Date类型的属性格式化为"yyyy-MM-dd HH:mm:ss"字符串。
     */
    @JsonFormat(pattern = "HH:mm", timezone = "GMT+8")
    private Date arrivalTime;

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
     * 车票类型 0：成人 1：儿童 2：学生 3：残疾军人
     */
    private Integer ticketType;

    /**
     * 订单金额
     */
    private Integer amount;
}
