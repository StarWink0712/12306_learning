package com.guoxu.userservice.service.impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.guoxu.DistributedCache;
import com.guoxu.chain.AbstractChainContext;
import com.guoxu.exception.ClientException;
import com.guoxu.exception.ServiceException;
import com.guoxu.toolkit.BeanUtil;
import com.guoxu.userservice.common.enums.UserChainMarkEnum;
import com.guoxu.userservice.dao.entity.*;
import com.guoxu.userservice.dao.mapper.*;
import com.guoxu.userservice.dto.req.UserDeletionReqDTO;
import com.guoxu.userservice.dto.req.UserLoginReqDTO;
import com.guoxu.userservice.dto.req.UserRegisterReqDTO;
import com.guoxu.userservice.dto.resp.UserLoginRespDTO;
import com.guoxu.userservice.dto.resp.UserQueryRespDTO;
import com.guoxu.userservice.dto.resp.UserRegisterRespDTO;
import com.guoxu.userservice.service.UserLoginService;
import com.guoxu.userservice.service.UserService;
import core.UserContext;
import core.UserInfoDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import toolkit.JWTUtil;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static com.guoxu.userservice.common.constant.RedisKeyConstant.*;
import static com.guoxu.userservice.common.enums.UserRegisterErrorCodeEnum.*;
import static com.guoxu.userservice.toolkit.UserReuseUtil.hashShardingIdx;

/**
 * UserLoginServiceImpl
 *
 * @author 执笔画棠
 * @date 2025/11/18 16:55
 **/
@Slf4j
@Service
@RequiredArgsConstructor
public class UserLoginServiceImpl implements UserLoginService {

    // 依赖的用户服务接口
    private final UserService userService;
    // 用户数据持久层映射器
    private final UserMapper userMapper;
    // 用户重用数据映射器（用于账号注销回收）
    private final UserReuseMapper userReuseMapper;
    // 用户注销数据映射器
    private final UserDeletionMapper userDeletionMapper;
    // 用户手机号数据映射器
    private final UserPhoneMapper userPhoneMapper;
    // 用户邮箱数据映射器
    private final UserMailMapper userMailMapper;
    // Redis分布式锁客户端
    private final RedissonClient redissonClient;
    // 分布式缓存组件
    private final DistributedCache distributedCache;
    // 用户注册请求的校验责任链上下文
    private final AbstractChainContext<UserRegisterReqDTO> abstractChainContext;
    // 防止用户注册缓存穿透的布隆过滤器
    private final RBloomFilter<String> userRegisterCachePenetrationBloomFilter;

    /**
     * 用户登录方法
     * 处理用户登录请求，验证用户名/邮箱/手机号和密码，返回登录凭证（JWT）
     *
     * @param requestParam 包含登录凭证（用户名/邮箱/手机号）和密码的请求参数
     * @return 包含登录凭证（JWT）的响应参数
     */
    @Override
    public UserLoginRespDTO login(UserLoginReqDTO requestParam) {
        // 获取登录凭证（用户名/邮箱/手机号）
        String usernameOrMailOrPhone = requestParam.getUsernameOrMailOrPhone();
        // 标记是否为邮箱格式
        boolean mailFlag = false;

        // 遍历字符串查找'@'字符判断邮箱格式（时间复杂度O(n)）
        for (char c : usernameOrMailOrPhone.toCharArray()) {
            if (c == '@') {
                mailFlag = true;
                break;
            }
        }

        String username;
        if (mailFlag) {
            // 构建邮箱查询条件
            LambdaQueryWrapper<UserMailDO> queryWrapper = Wrappers.lambdaQuery(UserMailDO.class)
                    .eq(UserMailDO::getMail, usernameOrMailOrPhone);
            // 根据邮箱查询用户名
            username = Optional.ofNullable(userMailMapper.selectOne(queryWrapper))
                    .map(UserMailDO::getUsername)
                    .orElseThrow(() -> new ClientException("用户名/手机号/邮箱不存在"));
        } else {
            // 构建手机号查询条件
            LambdaQueryWrapper<UserPhoneDO> queryWrapper = Wrappers.lambdaQuery(UserPhoneDO.class)
                    .eq(UserPhoneDO::getPhone, usernameOrMailOrPhone);
            // 根据手机号查询用户名
            username = Optional.ofNullable(userPhoneMapper.selectOne(queryWrapper))
                    .map(UserPhoneDO::getUsername)
                    .orElse(null);
        }

        // 如果通过手机号未查询到，则使用原始输入作为用户名
        username = Optional.ofNullable(username).orElse(requestParam.getUsernameOrMailOrPhone());

        // 构建用户主表查询条件
        LambdaQueryWrapper<UserDO> queryWrapper = Wrappers.lambdaQuery(UserDO.class)
                .eq(UserDO::getUsername, username)
                .eq(UserDO::getPassword, requestParam.getPassword())
                .select(UserDO::getId, UserDO::getUsername, UserDO::getRealName);
        // 查询用户信息
        UserDO userDO = userMapper.selectOne(queryWrapper);

        if (userDO != null) {
            // 构建用户信息DTO
            UserInfoDTO userInfo = UserInfoDTO.builder()
                    .userId(String.valueOf(userDO.getId()))
                    .username(userDO.getUsername())
                    .realName(userDO.getRealName())
                    .build();

            // 生成JWT访问令牌
            String accessToken = JWTUtil.generateAccessToken(userInfo);
            // 构建登录响应对象
            UserLoginRespDTO userLogin = UserLoginRespDTO.builder()
                    .userId(userInfo.getUserId())
                    .username(userInfo.getUsername())
                    .realName(userInfo.getRealName())
                    .accessToken(accessToken)
                    .build();

            // 将登录信息存入分布式缓存（30分钟过期）
            distributedCache.put(accessToken, JSON.toJSONString(userLogin), 30, TimeUnit.MINUTES);
            return userLogin;
        }
        // 认证失败抛出异常
        throw new ServiceException("账号不存在或密码错误");
    }

    @Override
    public UserLoginRespDTO checkLogin(String accessToken) {
        // 根据访问令牌从缓存获取登录信息并反序列化
        return distributedCache.get(accessToken, UserLoginRespDTO.class);
    }

    @Override
    public void logout(String accessToken) {
        // 非空检查后删除缓存中的登录令牌
        if (StrUtil.isNotBlank(accessToken)) {
            distributedCache.delete(accessToken);
        }
    }

    @Override
    public Boolean hasUsername(String username) {
        // 先查布隆过滤器快速判断
        boolean hasUsername = userRegisterCachePenetrationBloomFilter.contains(username);
        if (hasUsername) {
            // 布隆过滤器存在时再查Redis分片集合确认
            StringRedisTemplate instance = (StringRedisTemplate) distributedCache.getInstance();
            return instance.opsForSet().isMember(USER_REGISTER_REUSE_SHARDING + hashShardingIdx(username), username);
        }
        // 布隆过滤器未命中直接返回true（允许注册）
        return true;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public UserRegisterRespDTO register(UserRegisterReqDTO requestParam) {
        // 执行注册前置校验责任链
        abstractChainContext.handler(UserChainMarkEnum.USER_REGISTER_FILTER.name(), requestParam);

        // 获取用户注册分布式锁
        RLock lock = redissonClient.getLock(LOCK_USER_REGISTER + requestParam.getUsername());
        boolean tryLock = lock.tryLock();
        if (!tryLock) {
            // 锁获取失败说明用户名已被占用
            throw new ServiceException(HAS_USERNAME_NOTNULL);
        }

        try {
            try {
                // 插入用户主表
                int inserted = userMapper.insert(BeanUtil.convert(requestParam, UserDO.class));
                if (inserted < 1) {
                    throw new ServiceException(USER_REGISTER_FAIL);
                }
            } catch (DuplicateKeyException dke) {
                // 捕获唯一键冲突（用户名重复）
                log.error("用户名 [{}] 重复注册", requestParam.getUsername());
                throw new ServiceException(HAS_USERNAME_NOTNULL);
            }

            // 构建并插入用户手机号关联记录
            UserPhoneDO userPhoneDO = UserPhoneDO.builder()
                    .phone(requestParam.getPhone())
                    .username(requestParam.getUsername())
                    .build();
            try {
                userPhoneMapper.insert(userPhoneDO);
            } catch (DuplicateKeyException dke) {
                // 捕获手机号重复异常
                log.error("用户 [{}] 注册手机号 [{}] 重复", requestParam.getUsername(), requestParam.getPhone());
                throw new ServiceException(PHONE_REGISTERED);
            }

            // 如果提供了邮箱则插入邮箱关联记录
            if (StrUtil.isNotBlank(requestParam.getMail())) {
                UserMailDO userMailDO = UserMailDO.builder()
                        .mail(requestParam.getMail())
                        .username(requestParam.getUsername())
                        .build();
                try {
                    userMailMapper.insert(userMailDO);
                } catch (DuplicateKeyException dke) {
                    // 捕获邮箱重复异常
                    log.error("用户 [{}] 注册邮箱 [{}] 重复", requestParam.getUsername(), requestParam.getMail());
                    throw new ServiceException(MAIL_REGISTERED);
                }
            }

            // 删除用户重用记录（如果存在）
            String username = requestParam.getUsername();
            userReuseMapper.delete(Wrappers.update(new UserReuseDO(username)));

            // 从Redis重用集合中移除该用户名
            StringRedisTemplate instance = (StringRedisTemplate) distributedCache.getInstance();
            instance.opsForSet().remove(USER_REGISTER_REUSE_SHARDING + hashShardingIdx(username), username);

            // 将用户名加入布隆过滤器防止缓存穿透
            userRegisterCachePenetrationBloomFilter.add(username);
        } finally {
            // 确保释放锁
            lock.unlock();
        }
        // 返回注册结果DTO
        return BeanUtil.convert(requestParam, UserRegisterRespDTO.class);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void deletion(UserDeletionReqDTO requestParam) {
        // 获取当前登录用户名
        String username = UserContext.getUsername();
        // 校验注销账号与登录账号一致性
        if (!Objects.equals(username, requestParam.getUsername())) {
            throw new ClientException("注销账号与登录账号不一致");
        }

        // 获取用户注销分布式锁
        RLock lock = redissonClient.getLock(USER_DELETION + requestParam.getUsername());
        lock.lock(); // 加锁（根据文档要求放在try外）
        try {
            // 查询用户完整信息
            UserQueryRespDTO userQueryRespDTO = userService.queryUserByUsername(username);

            // 插入用户注销记录
            UserDeletionDO userDeletionDO = UserDeletionDO.builder()
                    .idType(userQueryRespDTO.getIdType())
                    .idCard(userQueryRespDTO.getIdCard())
                    .build();
            userDeletionMapper.insert(userDeletionDO);

            // 标记用户主表删除状态（软删除）
            UserDO userDO = new UserDO();
            userDO.setDeletionTime(System.currentTimeMillis());
            userDO.setUsername(username);
            userMapper.deletionUser(userDO);

            // 标记手机号关联记录删除状态
            UserPhoneDO userPhoneDO = UserPhoneDO.builder()
                    .phone(userQueryRespDTO.getPhone())
                    .deletionTime(System.currentTimeMillis())
                    .build();
            userPhoneMapper.deletionUser(userPhoneDO);

            // 如果存在邮箱则标记邮箱关联记录删除状态
            if (StrUtil.isNotBlank(userQueryRespDTO.getMail())) {
                UserMailDO userMailDO = UserMailDO.builder()
                        .mail(userQueryRespDTO.getMail())
                        .deletionTime(System.currentTimeMillis())
                        .build();
                userMailMapper.deletionUser(userMailDO);
            }

            // 删除当前用户的访问令牌
            distributedCache.delete(UserContext.getToken());

            // 插入用户重用记录（供后续账号回收使用）
            userReuseMapper.insert(new UserReuseDO(username));

            // 将用户名加入Redis重用集合
            StringRedisTemplate instance = (StringRedisTemplate) distributedCache.getInstance();
            instance.opsForSet().add(USER_REGISTER_REUSE_SHARDING + hashShardingIdx(username), username);
        } finally {
            // 确保释放锁
            lock.unlock();
        }
    }
}