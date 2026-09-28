package com.guoxu.userservice.dto.req;

import lombok.Data;

/**
 * UserUpdateReqDTO
 * 用户修改请求参数
 * @author 执笔画棠
 * @date 2025/11/18 16:27
 **/
@Data
public class UserUpdateReqDTO {
    /**
     * 用户ID
     */
    private String id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 邮箱
     */
    private String mail;

    /**
     * 旅客类型
     */
    private Integer userType;

    /**
     * 邮编
     */
    private String postCode;

    /**
     * 地址
     */
    private String address;
}
