package com.guoxu.core.token;

import com.guoxu.Results;

import lombok.RequiredArgsConstructor;
import org.opengoofy.index12306.framework.starter.convention.result.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * IdempotentTokenController
 *
 * @author 执笔画棠
 * @date 2025/11/06 20:17
 **/
@RestController
@RequiredArgsConstructor
public class IdempotentTokenController {

    private final IdempotentTokenService idempotentTokenService;

    //请求申请token
    @GetMapping("/token")
    public Result<String> createToken(){
        return Results.success(idempotentTokenService.createToken());
    }
}
