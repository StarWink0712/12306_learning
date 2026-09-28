package com.guoxu.userservice.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guoxu.userservice.dao.entity.UserDO;

/**
 * UserMapper
 * 用户信息持久层
 * @author 执笔画棠
 * @version 2025/11/18 16:21
 **/
public interface UserMapper extends BaseMapper<UserDO> {
    /**
     * 注销用户
     *
     * @param userDO 注销用户入参
     */
    void deletionUser(UserDO userDO);
}