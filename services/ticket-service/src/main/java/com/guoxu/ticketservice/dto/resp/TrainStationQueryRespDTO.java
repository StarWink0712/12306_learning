package com.guoxu.ticketservice.dto.resp;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * TrainStationQueryRespDTO
 * 列车站点查询响应参数
 * @author 执笔画棠
 * @date 2025/11/12 21:26
 **/
@Data
public class TrainStationQueryRespDTO {
    /**
     * 站序
     */
    private String sequence;

    /**
     * 站名
     */
    private String departure;

    /**
     * 到站时间
     */
    @JsonFormat(pattern = "HH:mm", timezone = "GMT+8")
    private Date arrivalTime;

    /**
     * 出发时间
     */
    @JsonFormat(pattern = "HH:mm", timezone = "GMT+8")
    private Date departureTime;

    /**
     * 停留时间
     */
    private Integer stopoverTime;
}
