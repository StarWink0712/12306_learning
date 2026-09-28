package com.guoxu.userservice.dto.req;

import lombok.Data;

/**
 * UserDeletionReqDTO
 * 用户注销请求参数
 * @author 执笔画棠
 * @date 2025/11/18 16:25
 **/
@Data
public class UserDeletionReqDTO {
    /**
     * 用户名
     */
    private String username;
}
