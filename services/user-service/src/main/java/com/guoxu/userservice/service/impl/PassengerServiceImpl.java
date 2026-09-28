package com.guoxu.userservice.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.IdcardUtil;
import cn.hutool.core.util.PhoneUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.toolkit.SqlHelper;
import com.guoxu.DistributedCache;
import com.guoxu.annotation.Idempotent;
import com.guoxu.enums.IdempotentTypeEnum;
import com.guoxu.exception.ClientException;
import com.guoxu.exception.ServiceException;
import com.guoxu.toolkit.BeanUtil;
import com.guoxu.userservice.common.enums.VerifyStatusEnum;
import com.guoxu.userservice.dao.entity.PassengerDO;
import com.guoxu.userservice.dao.mapper.PassengerMapper;
import com.guoxu.userservice.dto.req.PassengerRemoveReqDTO;
import com.guoxu.userservice.dto.req.PassengerReqDTO;
import com.guoxu.userservice.dto.resp.PassengerActualRespDTO;
import com.guoxu.userservice.dto.resp.PassengerRespDTO;
import com.guoxu.userservice.service.PassengerService;
import core.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.guoxu.userservice.common.constant.RedisKeyConstant.USER_PASSENGER_LIST;

/**
 * PassengerServiceImpl
 *
 * @author 执笔画棠
 * @date 2025/11/18 16:48
 **/
@Slf4j
@Service
@RequiredArgsConstructor
// 乘客服务实现类，实现 PassengerService 接口
public class PassengerServiceImpl implements PassengerService {

    // 乘客数据访问接口
    private final PassengerMapper passengerMapper;
    // 分布式缓存接口
    private final DistributedCache distributedCache;

    // 根据用户名查询乘客列表
    @Override
    public List<PassengerRespDTO> listPassengerQueryByUsername(String username) {
        // 获取实际的用户乘客列表字符串
        String actualUserPassengerListStr = getActualUserPassengerListStr(username);
        // 如果字符串不为空，将其解析为 PassengerDO 列表，再转换为 PassengerRespDTO 列表，否则返回 null
        return Optional.ofNullable(actualUserPassengerListStr)
                .map(each -> JSON.parseArray(each, PassengerDO.class))
                .map(each -> BeanUtil.convert(each, PassengerRespDTO.class))
                .orElse(null);
    }

    // 获取实际的用户乘客列表字符串，先从缓存获取，不存在则从数据库查询并缓存
    private String getActualUserPassengerListStr(String username) {
        return distributedCache.safeGet(
                // 缓存键，由 USER_PASSENGER_LIST 常量和用户名组成
                USER_PASSENGER_LIST + username,
                // 缓存值类型为 String
                String.class,
                () -> {
                    // 构建查询条件，查询指定用户名的乘客列表
                    LambdaQueryWrapper<PassengerDO> queryWrapper = Wrappers.lambdaQuery(PassengerDO.class)
                            .eq(PassengerDO::getUsername, username);
                    List<PassengerDO> passengerDOList = passengerMapper.selectList(queryWrapper);
                    // 如果查询结果不为空，将其转换为 JSON 字符串返回，否则返回 null
                    return CollUtil.isNotEmpty(passengerDOList)? JSON.toJSONString(passengerDOList) : null;
                },
                // 缓存有效期为 1 天
                1,
                TimeUnit.DAYS);
    }

    // 根据用户名和乘客 ID 列表查询乘客列表
    @Override
    public List<PassengerActualRespDTO> listPassengerQueryByIds(String username, List<Long> ids) {
        // 获取实际的用户乘客列表字符串
        String actualUserPassengerListStr = getActualUserPassengerListStr(username);
        // 如果字符串为空，返回 null
        if (StrUtil.isEmpty(actualUserPassengerListStr)) {
            return null;
        }
        // 将字符串解析为 PassengerDO 列表，过滤出符合 ID 列表的乘客，再转换为 PassengerActualRespDTO 列表
        return JSON.parseArray(actualUserPassengerListStr, PassengerDO.class)
                .stream().filter(passengerDO -> ids.contains(passengerDO.getId()))
                .map(each -> BeanUtil.convert(each, PassengerActualRespDTO.class))
                .collect(Collectors.toList());
    }

    // 保存乘客信息
    @Override
    public void savePassenger(PassengerReqDTO requestParam) {
        // 验证乘客信息
        verifyPassenger(requestParam);
        // 获取当前用户的用户名
        String username = UserContext.getUsername();
        try {
            // 将请求参数转换为 PassengerDO 对象
            PassengerDO passengerDO = BeanUtil.convert(requestParam, PassengerDO.class);
            // 设置用户名
            passengerDO.setUsername(username);
            // 设置创建日期为当前日期
            passengerDO.setCreateDate(new Date());
            // 设置验证状态为已审核
            passengerDO.setVerifyStatus(VerifyStatusEnum.REVIEWED.getCode());
            // 插入乘客信息到数据库
            int inserted = passengerMapper.insert(passengerDO);
            // 如果插入失败，抛出服务异常
            if (!SqlHelper.retBool(inserted)) {
                throw new ServiceException(String.format("[%s] 新增乘车人失败", username));
            }
        } catch (Exception ex) {
            // 如果是服务异常，记录错误信息和请求参数
            if (ex instanceof ServiceException) {
                log.error("{}，请求参数：{}", ex.getMessage(), JSON.toJSONString(requestParam));
            } else {
                // 否则记录详细错误信息、用户名和请求参数
                log.error("[{}] 新增乘车人失败，请求参数：{}", username, JSON.toJSONString(requestParam), ex);
            }
            // 抛出异常
            throw ex;
        }
        // 删除用户乘客缓存
        delUserPassengerCache(username);
    }

    // 更新乘客信息
    @Override
    public void updatePassenger(PassengerReqDTO requestParam) {
        // 验证乘客信息
        verifyPassenger(requestParam);
        // 获取当前用户的用户名
        String username = UserContext.getUsername();
        try {
            // 将请求参数转换为 PassengerDO 对象
            PassengerDO passengerDO = BeanUtil.convert(requestParam, PassengerDO.class);
            // 设置用户名
            passengerDO.setUsername(username);
            // 构建更新条件，更新指定用户名和 ID 的乘客信息
            LambdaUpdateWrapper<PassengerDO> updateWrapper = Wrappers.lambdaUpdate(PassengerDO.class)
                    .eq(PassengerDO::getUsername, username)
                    .eq(PassengerDO::getId, requestParam.getId());
            // 更新乘客信息到数据库
            int updated = passengerMapper.update(passengerDO, updateWrapper);
            // 如果更新失败，抛出服务异常
            if (!SqlHelper.retBool(updated)) {
                throw new ServiceException(String.format("[%s] 修改乘车人失败", username));
            }
        } catch (Exception ex) {
            // 如果是服务异常，记录错误信息和请求参数
            if (ex instanceof ServiceException) {
                log.error("{}，请求参数：{}", ex.getMessage(), JSON.toJSONString(requestParam));
            } else {
                // 否则记录详细错误信息、用户名和请求参数
                log.error("[{}] 修改乘车人失败，请求参数：{}", username, JSON.toJSONString(requestParam), ex);
            }
            // 抛出异常
            throw ex;
        }
        // 删除用户乘客缓存
        delUserPassengerCache(username);
    }

    // 移除乘客信息，使用幂等性注解确保操作幂等
    @Idempotent(uniqueKeyPrefix = "index12306 - user:lock_passenger - alter:", key = "T(org.opengoofy.index12306.frameworks.starter.user.core.UserContext).getUsername()", type = IdempotentTypeEnum.SPEL, scene = IdempotentSceneEnum.RESTAPI, message = "正在移除乘车人，请稍后再试...")
    @Override
    public void removePassenger(PassengerRemoveReqDTO requestParam) {
        // 获取当前用户的用户名
        String username = UserContext.getUsername();
        // 根据用户名和乘客 ID 查询乘客信息
        PassengerDO passengerDO = selectPassenger(username, requestParam.getId());
        // 如果乘客信息不存在，抛出客户端异常
        if (Objects.isNull(passengerDO)) {
            throw new ClientException("乘车人数据不存在");
        }
        try {
            // 构建删除条件，删除指定用户名和 ID 的乘客信息（逻辑删除）
            LambdaUpdateWrapper<PassengerDO> deleteWrapper = Wrappers.lambdaUpdate(PassengerDO.class)
                    .eq(PassengerDO::getUsername, username)
                    .eq(PassengerDO::getId, requestParam.getId());
            // 执行删除操作（逻辑删除，修改数据库表记录 del_flag）
            int deleted = passengerMapper.delete(deleteWrapper);
            // 如果删除失败，抛出服务异常
            if (!SqlHelper.retBool(deleted)) {
                throw new ServiceException(String.format("[%s] 删除乘车人失败", username));
            }
        } catch (Exception ex) {
            // 如果是服务异常，记录错误信息和请求参数
            if (ex instanceof ServiceException) {
                log.error("{}，请求参数：{}", ex.getMessage(), JSON.toJSONString(requestParam));
            } else {
                // 否则记录详细错误信息、用户名和请求参数
                log.error("[{}] 删除乘车人失败，请求参数：{}", username, JSON.toJSONString(requestParam), ex);
            }
            // 抛出异常
            throw ex;
        }
        // 删除用户乘客缓存
        delUserPassengerCache(username);
    }

    // 根据用户名和乘客 ID 查询乘客信息
    private PassengerDO selectPassenger(String username, String passengerId) {
        // 构建查询条件，查询指定用户名和 ID 的乘客信息
        LambdaQueryWrapper<PassengerDO> queryWrapper = Wrappers.lambdaQuery(PassengerDO.class)
                .eq(PassengerDO::getUsername, username)
                .eq(PassengerDO::getId, passengerId);
        // 返回查询结果
        return passengerMapper.selectOne(queryWrapper);
    }

    // 删除用户乘客缓存
    private void delUserPassengerCache(String username) {
        // 从分布式缓存中删除指定键的缓存数据
        distributedCache.delete(USER_PASSENGER_LIST + username);
    }

    // 验证乘客信息
    private void verifyPassenger(PassengerReqDTO requestParam) {
        // 获取乘客姓名长度
        int length = requestParam.getRealName().length();
        // 验证姓名长度是否在 2 到 16 位之间，否则抛出客户端异常
        if (!(length >= 2 && length <= 16)) {
            throw new ClientException("乘车人名称请设置 2 - 16 位的长度");
        }
        // 验证身份证号是否有效，否则抛出客户端异常
        if (!IdcardUtil.isValidCard(requestParam.getIdCard())) {
            throw new ClientException("乘车人证件号错误");
        }
        // 验证手机号是否合法，否则抛出客户端异常
        if (!PhoneUtil.isMobile(requestParam.getPhone())) {
            throw new ClientException("乘车人手机号错误");
        }
    }
}