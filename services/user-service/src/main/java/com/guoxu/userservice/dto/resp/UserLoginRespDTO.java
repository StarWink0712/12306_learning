package com.guoxu.userservice.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * UserLoginRespDTO
 * 用户登录返回参数
 * @author 执笔画棠
 * @date 2025/11/18 16:28
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserLoginRespDTO {
    /**
     * 用户 ID
     */
    private String userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 真实姓名
     */
    private String realName;

    /**
     * Token
     */
    private String accessToken;
}
