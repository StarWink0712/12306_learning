package com.guoxu.userservice.service.handler;

import com.guoxu.exception.ClientException;
import com.guoxu.userservice.common.enums.UserRegisterErrorCodeEnum;
import com.guoxu.userservice.dto.req.UserRegisterReqDTO;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * UserRegisterParamNotNullChainHandler
 *
 * @author 执笔画棠
 * @date 2025/11/18 16:48
 **/
@Component
// 用户注册参数不能为空责任链处理器类，实现UserRegisterCreateChainFilter接口
public class UserRegisterParamNotNullChainHandler implements UserRegisterCreateChainFilter<UserRegisterReqDTO>{

    /*
     *该类实现了 UserRegisterCreateChainFilter 接口，用于在用户注册流程中检查请求参数是否为空。
     * 通过 handler 方法依次检查用户名、密码、手机号、证件类型、证件号码、邮箱和真实姓名等参数，
     * 只要有一个参数为空，就抛出相应的客户端异常。getOrder 方法返回 0，表示该处理器在责任链中优先级最高，会最先执行参数检查。
     */


    // 处理用户注册请求参数的方法，检查各参数是否为空
    @Override
    public void handler(UserRegisterReqDTO requestParam) {
        // 检查用户名是否为空，若为空则抛出客户端异常，提示用户名不能为空
        if (Objects.isNull(requestParam.getUsername())) {
            throw new ClientException(UserRegisterErrorCodeEnum.USER_NAME_NOTNULL);
            // 检查密码是否为空，若为空则抛出客户端异常，提示密码不能为空
        } else if (Objects.isNull(requestParam.getPassword())) {
            throw new ClientException(UserRegisterErrorCodeEnum.PASSWORD_NOTNULL);
            // 检查手机号是否为空，若为空则抛出客户端异常，提示手机号不能为空
        } else if (Objects.isNull(requestParam.getPhone())) {
            throw new ClientException(UserRegisterErrorCodeEnum.PHONE_NOTNULL);
            // 检查证件类型是否为空，若为空则抛出客户端异常，提示证件类型不能为空
        } else if (Objects.isNull(requestParam.getIdType())) {
            throw new ClientException(UserRegisterErrorCodeEnum.ID_TYPE_NOTNULL);
            // 检查证件号码是否为空，若为空则抛出客户端异常，提示证件号码不能为空
        } else if (Objects.isNull(requestParam.getIdCard())) {
            throw new ClientException(UserRegisterErrorCodeEnum.ID_CARD_NOTNULL);
            // 检查邮箱是否为空，若为空则抛出客户端异常，提示邮箱不能为空
        } else if (Objects.isNull(requestParam.getMail())) {
            throw new ClientException(UserRegisterErrorCodeEnum.MAIL_NOTNULL);
            // 检查真实姓名是否为空，若为空则抛出客户端异常，提示真实姓名不能为空
        } else if (Objects.isNull(requestParam.getRealName())) {
            throw new ClientException(UserRegisterErrorCodeEnum.REAL_NAME_NOTNULL);
        }
    }

    // 获取该处理器在责任链中的执行顺序
    @Override
    public int getOrder() {
        return 0;
    }
}