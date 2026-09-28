package com.guoxu.userservice.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guoxu.userservice.dao.entity.UserMailDO;

/**
 * UserMailMapper 用户邮箱持久层
 *
 * @author 执笔画棠
 * @version 2025/11/18 16:20
 **/
public interface UserMailMapper extends BaseMapper<UserMailDO> {
    /**
     * 注销用户
     *
     * @param userMailDO 注销用户入参
     */
    void deletionUser(UserMailDO userMailDO);
}