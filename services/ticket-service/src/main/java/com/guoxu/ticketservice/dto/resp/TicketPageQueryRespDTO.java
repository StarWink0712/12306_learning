package com.guoxu.ticketservice.dto.resp;

import com.guoxu.ticketservice.dto.domain.TicketListDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * TicketPageQueryRespDTO
 * 车票分页查询响应参数
 * @author 执笔画棠
 * @date 2025/11/12 21:24
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TicketPageQueryRespDTO {
    /**
     * 车次集合数据
     */
    private List<TicketListDTO> trainList;

    /**
     * 车次类型：D-动车 Z-直达 复兴号等
     */
    private List<Integer> trainBrandList;

    /**
     * 出发车站
     */
    private List<String> departureStationList;

    /**
     * 到达车站
     */
    private List<String> arrivalStationList;

    /**
     * 车次席别
     */
    private List<Integer> seatClassTypeList;
}
