package com.guoxu.ticketservice.dto.req;

import com.guoxu.ticketservice.dto.domain.PurchaseTicketPassengerDetailDTO;
import lombok.Data;

import java.util.List;

/**
 * PurchaseTicketReqDTO
 * 购票请求入参
 * @author 执笔画棠
 * @date 2025/11/12 21:16
 **/
@Data
public class PurchaseTicketReqDTO {

    /**
     * 车次 ID
     */
    private String trainId;

    /**
     * 乘车人
     */
    private List<PurchaseTicketPassengerDetailDTO> passengers;

    /**
     * 选择座位
     */
    private List<String> chooseSeats;

    /**
     * 出发站点
     */
    private String departure;

    /**
     * 到达站点
     */
    private String arrival;
}
