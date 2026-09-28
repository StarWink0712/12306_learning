package com.guoxu.userservice.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoxu.base.BaseDo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * UserPhoneDO
 *
 * @author 执笔画棠
 * @date 2025/11/18 16:16
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@TableName("t_user_phone")
public class UserPhoneDO extends BaseDo {
    /**
     * id
     */
    private Long id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 注销时间戳
     */
    private Long deletionTime;
}
