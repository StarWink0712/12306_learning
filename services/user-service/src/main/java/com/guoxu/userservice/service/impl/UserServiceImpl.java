package com.guoxu.userservice.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.guoxu.exception.ClientException;
import com.guoxu.toolkit.BeanUtil;
import com.guoxu.userservice.dao.entity.UserDO;
import com.guoxu.userservice.dao.entity.UserDeletionDO;
import com.guoxu.userservice.dao.entity.UserMailDO;
import com.guoxu.userservice.dao.mapper.UserDeletionMapper;
import com.guoxu.userservice.dao.mapper.UserMailMapper;
import com.guoxu.userservice.dao.mapper.UserMapper;
import com.guoxu.userservice.dto.req.UserUpdateReqDTO;
import com.guoxu.userservice.dto.resp.UserQueryActualRespDTO;
import com.guoxu.userservice.dto.resp.UserQueryRespDTO;
import com.guoxu.userservice.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;

/**
 * UserServiceImpl
 * 用户信息接口实现层
 * @author 执笔画棠
 * @date 2025/11/18 16:54
 **/
@Service
@RequiredArgsConstructor
// 用户服务实现类，实现 UserService 接口
public class UserServiceImpl implements UserService {

    // 用户数据访问接口
    private final UserMapper userMapper;
    // 用户删除记录数据访问接口
    private final UserDeletionMapper userDeletionMapper;
    // 用户邮箱数据访问接口
    private final UserMailMapper userMailMapper;

    // 根据用户ID查询用户信息
    @Override
    public UserQueryRespDTO queryUserByUserId(String userId) {
        // 构建查询条件，根据用户ID查询用户
        LambdaQueryWrapper<UserDO> queryWrapper = Wrappers.lambdaQuery(UserDO.class)
                .eq(UserDO::getId, userId);
        // 从数据库中查询单个用户
        UserDO userDO = userMapper.selectOne(queryWrapper);
        // 如果用户不存在，抛出客户端异常
        if (userDO == null) {
            throw new ClientException("用户不存在，请检查用户ID是否正确");
        }
        // 将查询到的用户数据转换为 UserQueryRespDTO 并返回
        return BeanUtil.convert(userDO, UserQueryRespDTO.class);
    }

    // 根据用户名查询用户信息
    @Override
    public UserQueryRespDTO queryUserByUsername(String username) {
        // 构建查询条件，根据用户名查询用户
        LambdaQueryWrapper<UserDO> queryWrapper = Wrappers.lambdaQuery(UserDO.class)
                .eq(UserDO::getUsername, username);
        // 从数据库中查询单个用户
        UserDO userDO = userMapper.selectOne(queryWrapper);
        // 如果用户不存在，抛出客户端异常
        if (userDO == null) {
            throw new ClientException("用户不存在，请检查用户名是否正确");
        }
        // 将查询到的用户数据转换为 UserQueryRespDTO 并返回
        return BeanUtil.convert(userDO, UserQueryRespDTO.class);
    }

    // 根据用户名查询实际用户信息，通过转换 queryUserByUsername 的结果实现
    @Override
    public UserQueryActualRespDTO queryActualUserByUsername(String username) {
        return BeanUtil.convert(queryUserByUsername(username), UserQueryActualRespDTO.class);
    }

    // 根据证件类型和证件号查询用户删除记录数量
    @Override
    public Integer queryUserDeletionNum(Integer idType, String idCard) {
        // 构建查询条件，根据证件类型和证件号查询用户删除记录
        LambdaQueryWrapper<UserDeletionDO> queryWrapper = Wrappers.lambdaQuery(UserDeletionDO.class)
                .eq(UserDeletionDO::getIdType, idType)
                .eq(UserDeletionDO::getIdCard, idCard);
        // TODO 此处应该先查缓存
        // 从数据库中查询符合条件的用户删除记录数量
        Long deletionCount = userDeletionMapper.selectCount(queryWrapper);
        // 将 Long 类型的数量转换为 Integer 类型，如果为 null 则返回 0
        return Optional.ofNullable(deletionCount).map(Long::intValue).orElse(0);
    }

    // 更新用户信息 这里要用事务注解
    @Transactional
    @Override
    public void update(UserUpdateReqDTO requestParam) {
        // 根据用户名查询用户信息
        UserQueryRespDTO userQueryRespDTO = queryUserByUsername(requestParam.getUsername());
        // 将请求参数转换为 UserDO 对象
        UserDO userDO = BeanUtil.convert(requestParam, UserDO.class);
        // 构建更新条件，根据用户名更新用户信息
        LambdaUpdateWrapper<UserDO> userUpdateWrapper = Wrappers.lambdaUpdate(UserDO.class)
                .eq(UserDO::getUsername, requestParam.getUsername());
        // 更新用户信息到数据库
        userMapper.update(userDO, userUpdateWrapper);
        // 如果请求参数中的邮箱不为空且与原邮箱不同
        if (StrUtil.isNotBlank(requestParam.getMail()) &&!Objects.equals(requestParam.getMail(), userQueryRespDTO.getMail())) {
            // 构建删除条件，删除原邮箱记录
            LambdaUpdateWrapper<UserMailDO> updateWrapper = Wrappers.lambdaUpdate(UserMailDO.class)
                    .eq(UserMailDO::getMail, userQueryRespDTO.getMail());
            userMailMapper.delete(updateWrapper);
            // 创建新的用户邮箱对象
            UserMailDO userMailDO = UserMailDO.builder()
                    .mail(requestParam.getMail())
                    .username(requestParam.getUsername())
                    .build();
            // 插入新的用户邮箱记录到数据库
            userMailMapper.insert(userMailDO);
        }
    }
}