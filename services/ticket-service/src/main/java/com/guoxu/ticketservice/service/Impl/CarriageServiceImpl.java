package com.guoxu.ticketservice.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.guoxu.DistributedCache;
import com.guoxu.core.CacheLoader;
import com.guoxu.ticketservice.dao.entity.CarriageDO;
import com.guoxu.ticketservice.dao.mapper.CarriageMapper;
import com.guoxu.ticketservice.service.CarriageService;
import com.guoxu.toolkit.CacheUtil;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import cn.hutool.core.util.StrUtil;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.guoxu.ticketservice.common.constant.RedisKeyConstant.LOCK_QUERY_CARRIAGE_NUMBER_LIST;
import static com.guoxu.ticketservice.common.constant.RedisKeyConstant.TRAIN_CARRIAGE;

/**
 * CarriageServiceImpl
 * 列车车厢接口层实现
 * @author 执笔画棠
 * @date 2025/11/13 20:30
 **/
@Service
@RequiredArgsConstructor
public class CarriageServiceImpl implements CarriageService {
    // 注入分布式缓存对象，用于操作缓存
    private final DistributedCache distributedCache;
    // 注入车厢数据访问对象，用于数据库操作
    private final CarriageMapper carriageMapper;
    // 注入RedissonClient，用于获取分布式锁
    private final RedissonClient redissonClient;

    // 获取指定列车和车厢类型的车厢号列表
    @Override
    public List<String> listCarriageNumber(String trainId, Integer carriageType) {
        // 构建缓存键，格式为"TRAIN_CARRIAGE + trainId"
        final String key = TRAIN_CARRIAGE + trainId;
        // 安全地获取车厢号列表，若缓存中没有则从数据库加载并更新缓存
        return safeGetCarriageNumber(
                trainId,
                key,
                carriageType,
                () -> {
                    // 构建查询条件，查询指定列车和车厢类型的车厢数据
                    LambdaQueryWrapper<CarriageDO> queryWrapper = Wrappers.lambdaQuery(CarriageDO.class)
                            .eq(CarriageDO::getTrainId, trainId)
                            .eq(CarriageDO::getCarriageType, carriageType);
                    // 从数据库中查询符合条件的车厢数据列表
                    List<CarriageDO> carriageDOList = carriageMapper.selectList(queryWrapper);
                    // 提取车厢号并收集到一个列表中
                    List<String> carriageListWithOnlyNumber = carriageDOList.stream().map(CarriageDO::getCarriageNumber)
                            .collect(Collectors.toList());
                    // 将车厢号列表转换为以逗号分隔的字符串
                    return StrUtil.join(StrUtil.COMMA, carriageListWithOnlyNumber);
                });
    }

    // 获取用于操作哈希类型缓存的HashOperations对象
    private HashOperations<String, Object, Object> getHashOperations() {
        // 从分布式缓存中获取StringRedisTemplate实例
        StringRedisTemplate stringRedisTemplate = (StringRedisTemplate) distributedCache.getInstance();
        // 返回操作哈希类型缓存的对象
        return stringRedisTemplate.opsForHash();
    }

    // 从缓存中获取指定车厢类型的车厢号字符串
    private String getCarriageNumber(final String key, Integer carriageType) {
        // 获取操作哈希类型缓存的对象
        HashOperations<String, Object, Object> hashOperations = getHashOperations();
        // 从缓存中获取指定车厢类型的车厢号，若不存在则返回空字符串
        return Optional.ofNullable(hashOperations.get(key, String.valueOf(carriageType))).map(Object::toString)
                .orElse("");
    }

    // 安全地获取车厢号列表，处理缓存未命中和并发访问的情况
    private List<String> safeGetCarriageNumber(String trainId, final String key, Integer carriageType,
                                               CacheLoader<String> loader) {
        // 从缓存中获取车厢号字符串
        String result = getCarriageNumber(key, carriageType);
        // 如果缓存中有数据，则将字符串按逗号分隔成列表并返回
        if (!CacheUtil.isNullOrBlank(result)) {
            return StrUtil.split(result, StrUtil.COMMA);
        }
        // 获取分布式锁，确保在并发环境下数据的一致性
        RLock lock = redissonClient.getLock(String.format(LOCK_QUERY_CARRIAGE_NUMBER_LIST, trainId));
        lock.lock();
        try {
            // 再次检查缓存中是否有数据，因为在获取锁期间可能其他线程已更新缓存
            if (CacheUtil.isNullOrBlank(result = getCarriageNumber(key, carriageType))) {
                // 从数据库加载车厢号数据并设置到缓存中
                if (CacheUtil.isNullOrBlank(result = loadAndSet(carriageType, key, loader))) {
                    // 如果加载的数据为空，则返回空列表
                    return Collections.emptyList();
                }
            }
        } finally {
            // 释放分布式锁
            lock.unlock();
        }
        // 将获取到的车厢号字符串按逗号分隔成列表并返回
        return StrUtil.split(result, StrUtil.COMMA);
    }

    // 从数据库加载车厢号数据并设置到缓存中
    private String loadAndSet(Integer carriageType, final String key, CacheLoader<String> loader) {
        // 调用传入的加载器从数据库加载车厢号数据
        String result = loader.load();
        // 如果加载的数据为空，则直接返回
        if (CacheUtil.isNullOrBlank(result)) {
            return result;
        }
        // 获取操作哈希类型缓存的对象
        HashOperations<String, Object, Object> hashOperations = getHashOperations();
        // 将加载的数据设置到缓存中，仅当缓存中不存在该数据时才设置
        hashOperations.putIfAbsent(key, String.valueOf(carriageType), result);
        // 返回加载的数据
        return result;
    }
}
