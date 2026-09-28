package com.guoxu.payservice.dto.base;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * RefundResponse 退款返回
 *
 * @author 执笔画棠
 * @date 2025/11/11 17:49
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public final class RefundResponse {
    /**
     * 退款状态
     */
    private Integer status;

    /**
     * 第三方交易凭证
     */
    private String tradeNo;
}
