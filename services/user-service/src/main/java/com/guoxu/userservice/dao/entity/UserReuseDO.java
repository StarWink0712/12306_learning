package com.guoxu.userservice.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoxu.base.BaseDo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * UserReuseDO
 *
 * @author 执笔画棠
 * @date 2025/11/18 16:17
 **/
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_user_reuse")
public class UserReuseDO extends BaseDo {
    /**
     * 用户名
     */
    private String username;
}
