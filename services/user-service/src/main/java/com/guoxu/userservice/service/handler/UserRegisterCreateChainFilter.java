package com.guoxu.userservice.service.handler;

import com.guoxu.chain.AbstractChainHandler;
import com.guoxu.userservice.common.enums.UserChainMarkEnum;
import com.guoxu.userservice.dto.req.UserRegisterReqDTO;

/**
 * UserRegisterCreateChainFilter
 * 用户注册责任链过滤器
 * @author 执笔画棠
 * @version 2025/11/18 16:46
 **/
public interface UserRegisterCreateChainFilter<T extends UserRegisterReqDTO>
        extends AbstractChainHandler<UserRegisterReqDTO> {
    @Override
    default String mark() {
        return UserChainMarkEnum.USER_REGISTER_FILTER.name();
    }
}