package com.guoxu;

import com.alibaba.fastjson2.JSON;
import com.google.common.collect.Lists;

import com.guoxu.config.RedisDistributedProperties;
import com.guoxu.core.CacheGetFilter;
import com.guoxu.core.CacheGetIfAbsent;
import com.guoxu.core.CacheLoader;
import com.guoxu.toolkit.CacheUtil;
import com.guoxu.toolkit.FastJson2Util;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import lombok.RequiredArgsConstructor;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * 分布式缓存之操作 Redis 模版代理
 * 底层通过 {@link RedissonClient}、{@link StringRedisTemplate} 完成外观接口行为
 * 提供了对 Redis 缓存的基本操作，包括获取、设置、删除缓存数据等。
 */
@RequiredArgsConstructor
public class StringRedisTemplateProxy implements DistributedCache {

    // 使用StringRedisTemplate操作Redis字符串数据
    private final StringRedisTemplate stringRedisTemplate;
    // Redis分布式缓存配置属性
    private final RedisDistributedProperties redisProperties;
    // Redisson客户端，用于分布式锁和布隆过滤器等功能
    private final RedissonClient redissonClient;

    // Lua脚本路径：用于批量判断key不存在时设置的脚本
    private static final String LUA_PUT_IF_ALL_ABSENT_SCRIPT_PATH = "lua/putIfAllAbsent.lua";
    // 安全获取分布式锁的key前缀
    private static final String SAFE_GET_DISTRIBUTED_LOCK_KEY_PREFIX = "safe_get_distributed_lock_get:";

    /**
     * 根据key获取缓存值，并转换为指定类型
     */
    @Override
    public <T> T get(String key, Class<T> clazz) {
        // 从Redis获取字符串值
        String value = stringRedisTemplate.opsForValue().get(key);
        // 如果目标类型就是String，直接返回
        if (String.class.isAssignableFrom(clazz)) {
            return (T) value;
        }
        // 使用FastJson2将JSON字符串转换为指定类型对象
        return JSON.parseObject(value, FastJson2Util.buildType(clazz));
    }

    /**
     * 将值放入缓存，使用配置的默认超时时间
     */
    @Override
    public void put(String key, Object value) {
        put(key, value, redisProperties.getValueTimeout());
    }



    /**
     * 使用Lua脚本原子性地批量设置key（当所有key都不存在时）
     */
    @Override
    public Boolean putIfAllAbsent(@NotNull Collection<String> keys) {
        // 使用单例模式获取Lua脚本实例，避免重复加载
        DefaultRedisScript<Boolean> actual = Singleton.get(LUA_PUT_IF_ALL_ABSENT_SCRIPT_PATH, () -> {
            // 创建Lua脚本实例
            DefaultRedisScript redisScript = new DefaultRedisScript();
            // 从classpath加载Lua脚本
            redisScript.setScriptSource(
                    new ResourceScriptSource(new ClassPathResource(LUA_PUT_IF_ALL_ABSENT_SCRIPT_PATH)));
            // 设置脚本返回类型为Boolean
            redisScript.setResultType(Boolean.class);
            return redisScript;
        });
        // 执行Lua脚本：keys列表和超时时间作为参数
        Boolean result = stringRedisTemplate.execute(actual, Lists.newArrayList(keys),
                redisProperties.getValueTimeout().toString());
        // 返回执行结果，非空且为true表示成功
        return result != null && result;
    }

    /**
     * 删除单个key
     */
    @Override
    public Boolean delete(String key) {
        return stringRedisTemplate.delete(key);
    }

    /**
     * 批量删除key
     */
    @Override
    public Long delete(Collection<String> keys) {
        return stringRedisTemplate.delete(keys);
    }

    /**
     * 获取缓存，如果不存在则通过cacheLoader加载并设置缓存
     */
    @Override
    public <T> T get(@NotBlank String key, Class<T> clazz, CacheLoader<T> cacheLoader, long timeout) {
        return get(key, clazz, cacheLoader, timeout, redisProperties.getValueTimeUnit());
    }

    /**
     * 获取缓存，如果不存在则通过cacheLoader加载并设置缓存（可指定时间单位）
     */
    @Override
    public <T> T get(@NotBlank String key, Class<T> clazz, CacheLoader<T> cacheLoader, long timeout,
                     TimeUnit timeUnit) {
        // 先尝试从缓存获取
        T result = get(key, clazz);
        // 如果缓存中存在有效值，直接返回
        if (!CacheUtil.isNullOrBlank(result)) {
            return result;
        }
        // 缓存不存在，通过cacheLoader加载数据并设置到缓存
        return loadAndSet(key, cacheLoader, timeout, timeUnit, false, null);
    }

    /**
     * 安全获取缓存（带分布式锁保护）
     */
    @Override
    public <T> T safeGet(@NotBlank String key, Class<T> clazz, CacheLoader<T> cacheLoader, long timeout) {
        return safeGet(key, clazz, cacheLoader, timeout, redisProperties.getValueTimeUnit());
    }

    /**
     * 安全获取缓存（带分布式锁保护，可指定时间单位）
     */
    @Override
    public <T> T safeGet(@NotBlank String key, Class<T> clazz, CacheLoader<T> cacheLoader, long timeout,
                         TimeUnit timeUnit) {
        return safeGet(key, clazz, cacheLoader, timeout, timeUnit, null);
    }

    /**
     * 安全获取缓存（带布隆过滤器）
     */
    @Override
    public <T> T safeGet(@NotBlank String key, Class<T> clazz, CacheLoader<T> cacheLoader, long timeout,
                         RBloomFilter<String> bloomFilter) {
        return safeGet(key, clazz, cacheLoader, timeout, bloomFilter, null, null);
    }

    /**
     * 安全获取缓存（带布隆过滤器，可指定时间单位）
     */
    @Override
    public <T> T safeGet(@NotBlank String key, Class<T> clazz, CacheLoader<T> cacheLoader, long timeout,
                         TimeUnit timeUnit, RBloomFilter<String> bloomFilter) {
        return safeGet(key, clazz, cacheLoader, timeout, timeUnit, bloomFilter, null, null);
    }

    /**
     * 安全获取缓存（带布隆过滤器和缓存过滤器）
     */
    @Override
    public <T> T safeGet(String key, Class<T> clazz, CacheLoader<T> cacheLoader, long timeout,
                         RBloomFilter<String> bloomFilter, CacheGetFilter<String> cacheCheckFilter) {
        return safeGet(key, clazz, cacheLoader, timeout, redisProperties.getValueTimeUnit(), bloomFilter,
                cacheCheckFilter, null);
    }

    /**
     * 安全获取缓存（带布隆过滤器和缓存过滤器，可指定时间单位）
     */
    @Override
    public <T> T safeGet(String key, Class<T> clazz, CacheLoader<T> cacheLoader, long timeout, TimeUnit timeUnit,
                         RBloomFilter<String> bloomFilter, CacheGetFilter<String> cacheCheckFilter) {
        return safeGet(key, clazz, cacheLoader, timeout, timeUnit, bloomFilter, cacheCheckFilter, null);
    }

    /**
     * 安全获取缓存（带布隆过滤器、缓存过滤器和缓存缺失处理器）
     */
    @Override
    public <T> T safeGet(String key, Class<T> clazz, CacheLoader<T> cacheLoader, long timeout,
                         RBloomFilter<String> bloomFilter, CacheGetFilter<String> cacheGetFilter,
                         CacheGetIfAbsent<String> cacheGetIfAbsent) {
        return safeGet(key, clazz, cacheLoader, timeout, redisProperties.getValueTimeUnit(), bloomFilter,
                cacheGetFilter, cacheGetIfAbsent);
    }

    /**
     * 安全获取缓存 - 核心实现方法（带完整的缓存保护机制）
     */
    @Override
    public <T> T safeGet(String key, Class<T> clazz, CacheLoader<T> cacheLoader, long timeout, TimeUnit timeUnit,
                         RBloomFilter<String> bloomFilter, CacheGetFilter<String> cacheGetFilter,
                         CacheGetIfAbsent<String> cacheGetIfAbsent) {
        // 先尝试从缓存获取
        T result = get(key, clazz);
        // 缓存结果不等于空或空字符串直接返回；通过函数判断是否返回空，为了适配布隆过滤器无法删除的场景；两者都不成立，判断布隆过滤器是否存在，不存在返回空
        if (!CacheUtil.isNullOrBlank(result)
                || Optional.ofNullable(cacheGetFilter).map(each -> each.filter(key)).orElse(false)
                || Optional.ofNullable(bloomFilter).map(each -> !each.contains(key)).orElse(false)) {
            return result;
        }
        // 获取分布式锁，防止缓存击穿
        RLock lock = redissonClient.getLock(SAFE_GET_DISTRIBUTED_LOCK_KEY_PREFIX + key);
        lock.lock();
        try {
            // 双重判定锁，减轻获得分布式锁后线程访问数据库压力
            if (CacheUtil.isNullOrBlank(result = get(key, clazz))) {
                // 如果访问 cacheLoader 加载数据为空，执行后置函数操作
                if (CacheUtil
                        .isNullOrBlank(result = loadAndSet(key, cacheLoader, timeout, timeUnit, true, bloomFilter))) {
                    // 执行缓存缺失时的回调函数
                    Optional.ofNullable(cacheGetIfAbsent).ifPresent(each -> each.execute(key));
                }
            }
        } finally {
            // 释放分布式锁
            lock.unlock();
        }
        return result;
    }

    /**
     * 设置缓存值，指定超时时间（使用配置的时间单位）
     */
    @Override
    public void put(String key, Object value, long timeout) {
        put(key, value, timeout, redisProperties.getValueTimeUnit());
    }

    /**
     * 设置缓存值，指定超时时间和时间单位
     */
    @Override
    public void put(String key, Object value, long timeout, TimeUnit timeUnit) {
        // 如果value是String类型直接使用，否则转换为JSON字符串
        String actual = value instanceof String ? (String) value : JSON.toJSONString(value);
        // 设置到Redis，包含超时时间
        stringRedisTemplate.opsForValue().set(key, actual, timeout, timeUnit);
    }

    /**
     * 安全设置缓存（同时更新布隆过滤器）
     */
    @Override
    public void safePut(String key, Object value, long timeout, RBloomFilter<String> bloomFilter) {
        safePut(key, value, timeout, redisProperties.getValueTimeUnit(), bloomFilter);
    }

    /**
     * 安全设置缓存（同时更新布隆过滤器，可指定时间单位）
     */
    @Override
    public void safePut(String key, Object value, long timeout, TimeUnit timeUnit, RBloomFilter<String> bloomFilter) {
        // 设置缓存值
        put(key, value, timeout, timeUnit);
        // 如果布隆过滤器不为空，将key添加到布隆过滤器中
        if (bloomFilter != null) {
            bloomFilter.add(key);
        }
    }

    /**
     * 判断key是否存在
     */
    @Override
    public Boolean hasKey(String key) {
        return stringRedisTemplate.hasKey(key);
    }

    /**
     * 获取底层Redis模板实例
     */
    @Override
    public Object getInstance() {
        return stringRedisTemplate;
    }

    /**
     * 统计存在的key数量
     */
    @Override
    public Long countExistingKeys(String... keys) {
        return stringRedisTemplate.countExistingKeys(Lists.newArrayList(keys));
    }

    /**
     * 加载数据并设置到缓存的私有方法
     * @param key 缓存key
     * @param cacheLoader 数据加载器
     * @param timeout 超时时间
     * @param timeUnit 时间单位
     * @param safeFlag 是否安全模式（是否使用布隆过滤器）
     * @param bloomFilter 布隆过滤器
     * @return 加载的数据
     */
    private <T> T loadAndSet(String key, CacheLoader<T> cacheLoader, long timeout, TimeUnit timeUnit, boolean safeFlag,
                             RBloomFilter<String> bloomFilter) {
        // 通过cacheLoader加载数据
        T result = cacheLoader.load();
        // 如果加载的数据为空，直接返回
        if (CacheUtil.isNullOrBlank(result)) {
            return result;
        }
        // 根据安全模式标志选择不同的缓存设置方式
        if (safeFlag) {
            // 安全模式：设置缓存并更新布隆过滤器
            safePut(key, result, timeout, timeUnit, bloomFilter);
        } else {
            // 普通模式：仅设置缓存
            put(key, result, timeout, timeUnit);
        }
        return result;
    }
}