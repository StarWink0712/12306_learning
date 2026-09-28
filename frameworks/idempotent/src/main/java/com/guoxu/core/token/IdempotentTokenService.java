package com.guoxu.core.token;

import com.guoxu.core.IdempotentExecuteHandler;

/**
 * IdempotentTokenService
 *
 * @author 执笔画棠
 * @version 2025/11/06 20:17
 **/
public interface IdempotentTokenService extends IdempotentExecuteHandler {
    /**
     * 创建幂等验证Token
     */
    String createToken();
}