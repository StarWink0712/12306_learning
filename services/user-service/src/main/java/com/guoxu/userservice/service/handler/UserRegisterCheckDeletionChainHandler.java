package com.guoxu.userservice.service.handler;

import com.guoxu.exception.ClientException;
import com.guoxu.userservice.dto.req.UserRegisterReqDTO;
import com.guoxu.userservice.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * UserRegisterCheckDeletionChainHandler
 *
 * @author 执笔画棠
 * @date 2025/11/18 16:45
 **/
@Component
@RequiredArgsConstructor
// 这是一个最终类，实现了UserRegisterCreateChainFilter接口，用于在用户注册时检查用户注销次数
public final class UserRegisterCheckDeletionChainHandler implements UserRegisterCreateChainFilter<UserRegisterReqDTO> {

    /*
     *这个类作为责任链中的一个处理器，在用户注册流程中起到检查用户注销次数的作用。
     * 它依赖 UserService 获取特定证件类型和证件号的用户注销次数。
     * 若注销次数达到或超过 5 次，就会抛出异常阻止注册
     * 提示该证件号已因多次注销被加入黑名单。getOrder 方法返回2，
     * 表明它在责任链中的执行顺序为第 3 位（因为顺序从 0 开始） 。
     */

    // 依赖UserService服务，用于查询用户注销次数
    private final UserService userService;

    // 处理用户注册请求参数的方法，检查用户注销次数
    @Override
    public void handler(UserRegisterReqDTO requestParam) {
        // 通过UserService查询该证件类型和证件号对应的用户注销次数
        Integer userDeletionNum = userService.queryUserDeletionNum(requestParam.getIdType(), requestParam.getIdCard());
        // 如果注销次数大于等于5次
        if (userDeletionNum >= 5) {
            // 抛出客户端异常，提示该证件号因多次注销账号已被加入黑名单
            throw new ClientException("证件号多次注销账号已被加入黑名单");
        }
    }

    // 获取该处理器在责任链中的执行顺序
    @Override
    public int getOrder() {
        return 2;
    }
}