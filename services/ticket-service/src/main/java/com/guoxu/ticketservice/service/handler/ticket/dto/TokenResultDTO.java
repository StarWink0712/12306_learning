package com.guoxu.ticketservice.service.handler.ticket.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * TokenResultDTO 令牌扣减返回参数
 *
 * @author 执笔画棠
 * @date 2025/11/13 19:38
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TokenResultDTO {
    /**
     * Token 为空
     */
    private Boolean tokenIsNull;

    /**
     * 获取 Token 为空站点座位类型和数量
     */
    private List<String> tokenIsNullSeatTypeCounts;
}
