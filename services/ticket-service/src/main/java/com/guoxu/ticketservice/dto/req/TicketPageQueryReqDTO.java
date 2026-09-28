package com.guoxu.ticketservice.dto.req;

import com.guoxu.page.PageRequest;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

/**
 * TicketPageQueryReqDTO
 * 车票分页查询请求入参
 *
 * @author 执笔画棠
 * @date 2025/11/12 21:21
 **/
@Data
public class TicketPageQueryReqDTO extends PageRequest {
    /**
     * 出发地 Code
     */
    private String fromStation;

    /**
     * 目的地 Code
     */
    private String toStation;

    /**
     * 出发日期
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date departureDate;

    /**
     * 出发站点
     */
    private String departure;

    /**
     * 到达站点
     */
    private String arrival;
}
