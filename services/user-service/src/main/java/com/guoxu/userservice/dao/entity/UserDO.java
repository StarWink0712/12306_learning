package com.guoxu.userservice.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoxu.base.BaseDo;
import lombok.Data;

/**
 * UserDO
 *
 * @author 执笔画棠
 * @date 2025/11/18 16:13
 **/
@Data
@TableName("t_user")
public class UserDO extends BaseDo {

    /**
     * id
     */
    private Long id;

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
     * 国家/地区
     */
    private String region;

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
     * 固定电话
     */
    private String telephone;

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
    private Integer verifyStatus;

    /**
     * 邮编
     */
    private String postCode;

    /**
     * 地址
     */
    private String address;

    /**
     * 注销时间戳
     */
    private Long deletionTime;
}
