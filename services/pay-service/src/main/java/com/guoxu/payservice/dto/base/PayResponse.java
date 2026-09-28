package com.guoxu.payservice.dto.base;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * PayResponse
 * 支付返回
 * @author 执笔画棠
 * @date 2025/11/11 17:48
 **/
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public final class PayResponse {

    /**
     * 调用支付返回信息
     */
    private String body;
}
