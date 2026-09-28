package com.guoxu.userservice.controller;

import com.guoxu.Results;
import com.guoxu.result.Result;
import com.guoxu.userservice.dto.req.UserDeletionReqDTO;
import com.guoxu.userservice.dto.req.UserRegisterReqDTO;
import com.guoxu.userservice.dto.req.UserUpdateReqDTO;
import com.guoxu.userservice.dto.resp.UserQueryActualRespDTO;
import com.guoxu.userservice.dto.resp.UserQueryRespDTO;
import com.guoxu.userservice.dto.resp.UserRegisterRespDTO;
import com.guoxu.userservice.service.UserLoginService;
import com.guoxu.userservice.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * UserInfoController
 * 用户控制层
 * @author 执笔画棠
 * @date 2025/11/18 16:07
 **/
@RestController
@RequiredArgsConstructor
public class UserInfoController {

    private final UserLoginService userLoginService;
    private final UserService userService;

    /**
     * 根据用户名查询用户信息
     */
    @GetMapping("/api/user-service/query")
    public Result<UserQueryRespDTO> queryUserByUsername(@RequestParam("username") @NotEmpty String username) {
        return Results.success(userService.queryUserByUsername(username));
    }

    /**
     * 根据用户名查询用户无脱敏信息
     */
    @GetMapping("/api/user-service/actual/query")
    public Result<UserQueryActualRespDTO> queryActualUserByUsername(
            @RequestParam("username") @NotEmpty String username) {
        return Results.success(userService.queryActualUserByUsername(username));
    }

    /**
     * 检查用户名是否已存在
     */
    @GetMapping("/api/user-service/has-username")
    public Result<Boolean> hasUsername(@RequestParam("username") @NotEmpty String username) {
        return Results.success(userLoginService.hasUsername(username));
    }

    /**
     * 注册用户
     */
    @PostMapping("/api/user-service/register")
    public Result<UserRegisterRespDTO> register(@RequestBody @Valid UserRegisterReqDTO requestParam) {
        return Results.success(userLoginService.register(requestParam));
    }

    /**
     * 修改用户
     */
    @PostMapping("/api/user-service/update")
    public Result<Void> update(@RequestBody @Valid UserUpdateReqDTO requestParam) {
        userService.update(requestParam);
        return Results.success();
    }

    /**
     * 注销用户
     */
    @PostMapping("/api/user-service/deletion")
    public Result<Void> deletion(@RequestBody @Valid UserDeletionReqDTO requestParam) {
        userLoginService.deletion(requestParam);
        return Results.success();
    }
}
