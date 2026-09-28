package com.guoxu.userservice.service.handler;

import com.guoxu.exception.ClientException;
import com.guoxu.userservice.common.enums.UserRegisterErrorCodeEnum;
import com.guoxu.userservice.dto.req.UserRegisterReqDTO;
import com.guoxu.userservice.service.UserLoginService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * UserRegisterHasUsernameChainHandler
 *
 * @author 执笔画棠
 * @date 2025/11/18 16:47
 **/
@Component
@RequiredArgsConstructor
// 这是一个最终类，实现了UserRegisterCreateChainFilter接口，用于处理用户注册时用户名已存在的校验逻辑
public final class UserRegisterHasUsernameChainHandler implements UserRegisterCreateChainFilter<UserRegisterReqDTO>{

    /*
     *这个类在用户注册流程中扮演了一个责任链处理器的角色。
     * 它依赖 UserLoginService 来检查传入的注册请求参数中的用户名是否已存在。
     * 如果用户名不存在，就抛出一个客户端异常。
     * getOrder 方法定义了该处理器在责任链中的执行顺序为 1，这意味着它会在责任链的较靠前位置执行校验逻辑。
     */

    // 依赖用户登录服务，用于检查用户名是否已存在
    private final UserLoginService userLoginService;

    // 处理用户注册请求参数的方法，检查用户名是否已存在
    @Override
    public void handler(UserRegisterReqDTO requestParam) {
        // 如果用户名不存在，抛出客户端异常，提示用户名不能为空
        if (!userLoginService.hasUsername(requestParam.getUsername())) {
            throw new ClientException(UserRegisterErrorCodeEnum.HAS_USERNAME_NOTNULL);
        }
    }

    // 获取该处理器在责任链中的执行顺序
    @Override
    public int getOrder() {
        return 1;
    }
}