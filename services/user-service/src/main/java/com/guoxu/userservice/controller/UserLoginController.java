package com.guoxu.userservice.controller;

import com.guoxu.Results;
import com.guoxu.result.Result;
import com.guoxu.userservice.dto.req.UserLoginReqDTO;
import com.guoxu.userservice.dto.resp.UserLoginRespDTO;
import com.guoxu.userservice.service.UserLoginService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * UserLoginController
 *
 * @author 执笔画棠
 * @date 2025/11/18 16:09
 **/
@RestController
@RequiredArgsConstructor
public class UserLoginController {
    private final UserLoginService userLoginService;

    /**
     * 用户登录
     */
    @PostMapping("/api/user-service/v1/login")
    public Result<UserLoginRespDTO> login(@RequestBody UserLoginReqDTO requestParam) {
        return Results.success(userLoginService.login(requestParam));
    }

    /**
     * 通过 Token 检查用户是否登录
     */
    @GetMapping("/api/user-service/check-login")
    public Result<UserLoginRespDTO> checkLogin(@RequestParam("accessToken") String accessToken) {
        UserLoginRespDTO result = userLoginService.checkLogin(accessToken);
        return Results.success(result);
    }

    /**
     * 用户退出登录
     */
    @GetMapping("/api/user-service/logout")
    public Result<Void> logout(@RequestParam(required = false) String accessToken) {
        userLoginService.logout(accessToken);
        return Results.success();
    }
}
