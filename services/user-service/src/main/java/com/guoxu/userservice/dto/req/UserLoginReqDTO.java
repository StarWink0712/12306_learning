package com.guoxu.userservice.dto.req;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * UserLoginReqDTO
 *
 * @author 执笔画棠
 * @date 2025/11/18 16:25
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserLoginReqDTO {
    /**
     * 用户名
     */
    private String usernameOrMailOrPhone;

    /**
     * 密码
     */
    private String password;
}
