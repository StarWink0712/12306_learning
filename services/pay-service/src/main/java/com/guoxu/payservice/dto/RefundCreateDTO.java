package com.guoxu.payservice.dto;

import com.guoxu.payservice.remote.dto.TicketOrderPassengerDetailRespDTO;
import lombok.Data;

import java.util.List;

/**
 * RefundCreateDTO
 * 退款创建入参数实体
 * @author 执笔画棠
 * @date 2025/11/11 18:03
 **/
@Data
public class RefundCreateDTO {
    /**
     * 支付流水号
     */
    private String paySn;

    /**
     * 订单号
     */
    private String orderSn;

    /**
     * 退款类型
     */
    private Integer type;

    /**
     * 部分退款车票详情集合
     */
    private List<TicketOrderPassengerDetailRespDTO> refundDetailReqDTOList;
}
