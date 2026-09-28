package com.guoxu.ticketservice.remote.dto;

import com.guoxu.ticketservice.common.enums.RefundTypeEnum;
import lombok.Data;

/**
 * RefundReqDTO
 * 退款请求入参数实体
 * @author 执笔画棠
 * @date 2025/11/12 22:18
 **/
@Data
public class RefundReqDTO {
    /**
     * 订单号
     */
    private String orderSn;

    /**
     * 退款类型枚举
     */
    private RefundTypeEnum refundTypeEnum;

    /**
     * 退款金额
     */
    private Integer refundAmount;

    /**
     * 部分退款车票详情集合
     */
    private List<TicketOrderPassengerDetailRespDTO> refundDetailReqDTOList;
}
