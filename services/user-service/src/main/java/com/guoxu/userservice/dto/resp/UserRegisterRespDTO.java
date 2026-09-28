package com.guoxu.userservice.dto.resp;

import lombok.Data;

/**
 * UserRegisterRespDTO
 * 用户注册返回参数
 * @author 执笔画棠
 * @date 2025/11/18 16:30
 **/
@Data
public class UserRegisterRespDTO {
    /**
     * 用户名
     */
    private String username;

    /**
     * 真实姓名
     */
    private String realName;

    /**
     * 手机号
     */
    private String phone;
}
