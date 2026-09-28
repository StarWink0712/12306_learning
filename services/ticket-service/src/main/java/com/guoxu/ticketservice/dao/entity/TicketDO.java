package com.guoxu.ticketservice.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.ser.Serializers;
import com.guoxu.base.BaseDo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * TicketDO 车票实体
 *
 * @author 执笔画棠
 * @date 2025/11/12 20:33
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@TableName("t_ticket")
public class TicketDO extends BaseDo {

    /**
     * id
     */
    private Long id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 列车id
     */
    private Long trainId;

    /**
     * 车厢号
     */
    private String carriageNumber;

    /**
     * 座位号
     */
    private String seatNumber;

    /**
     * 乘车人 ID
     */
    private String passengerId;

    /**
     * 车票状态
     */
    private Integer ticketStatus;
}
