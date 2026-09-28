package com.guoxu.userservice.dto.req;

import lombok.Data;

/**
 * UserRegisterReqDTO
 * 用户注册请求参数
 * @author 执笔画棠
 * @date 2025/11/18 16:26
 **/
@Data
public class UserRegisterReqDTO {
    /**
     * 用户名
     */
    private String username;

    /**
     * 密码
     */
    private String password;

    /**
     * 真实姓名
     */
    private String realName;

    /**
     * 证件类型
     */
    private Integer idType;

    /**
     * 证件号
     */
    private String idCard;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 邮箱
     */
    private String mail;

    /**
     * 旅客类型
     */
    private Integer userType;

    /**
     * 审核状态
     */
    private Integer verifyState;

    /**
     * 邮编
     */
    private String postCode;

    /**
     * 地址
     */
    private String address;

    /**
     * 国家/地区
     */
    private String region;

    /**
     * 固定电话
     */
    private String telephone;
}
