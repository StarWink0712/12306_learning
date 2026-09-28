package com.guoxu.payservice.service;

import com.guoxu.payservice.dto.RefundReqDTO;
import com.guoxu.payservice.dto.RefundRespDTO;

/**
 * RefundService
 *
 * @author 执笔画棠
 * @version 2025/11/12 13:55
 **/
public interface RefundService {
    /**
     * 公共退款接口
     *
     * @param requestParam 退款请求参数
     * @return 退款返回详情
     */
    RefundRespDTO commonRefund(RefundReqDTO requestParam);
}