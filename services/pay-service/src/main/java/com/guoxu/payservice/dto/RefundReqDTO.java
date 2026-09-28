package com.guoxu.payservice.dto;

import com.guoxu.payservice.common.enums.RefundTypeEnum;
import com.guoxu.payservice.remote.dto.TicketOrderPassengerDetailRespDTO;
import lombok.Data;

import java.util.List;

/**
 * RefundReqDTO
 * 退款请求入参数实体
 * @author 执笔画棠
 * @date 2025/11/11 18:06
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
