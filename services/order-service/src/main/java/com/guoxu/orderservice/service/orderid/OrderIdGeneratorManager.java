package com.guoxu.orderservice.service.orderid;

import cn.crane4j.annotation.ContainerEnum;
import com.guoxu.DistributedCache;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * OrderIdGeneratorManager 全局订单唯一id生成器管理器
 *
 * @author 执笔画棠
 * @date 2025/11/09 21:35
 **/
@Component
@RequiredArgsConstructor
public class OrderIdGeneratorManager implements InitializingBean {

    //注入全局id生成器
    private static DistributedIdGenerator DISTRIBUTED_ID_GENERATOR;

    //注入redisson客户端
    private final RedissonClient redissonClient;

    //注入分布式缓存
    private final DistributedCache distributedCache;

    //生成全局唯一id
    public static String generateId(long userId){
        //订单 ID 结构：时间戳 | 节点 ID | 序列号 | 用户名后 6 位
        return DISTRIBUTED_ID_GENERATOR.generateId() + String.valueOf(userId%10000);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        // 定义一个用于分布式锁的 key，保证多个实例在初始化分布式 ID 生成器时不会并发冲突
        String LOCK_KEY = "distributed_id_generator_lock_key";

        // 从 Redisson 客户端中获取一个分布式锁对象
        RLock lock = redissonClient.getLock(LOCK_KEY);

        // 加锁操作，确保同一时刻只有一个节点可以执行下面的初始化逻辑
        lock.lock();

        try {
            // 从分布式缓存中获取 Redis 操作实例
            // distributedCache.getInstance() 返回的对象是 StringRedisTemplate，用于执行字符串操作
            StringRedisTemplate instance = (StringRedisTemplate) distributedCache.getInstance();

            // 定义一个 Redis 键，用于保存分布式 ID 生成器的全局配置（例如节点编号）
            String DISTRIBUTED_ID_GENERATOR_KEY = "distributed_id_generator_config";

            // 使用 Redis 的自增操作（INCR），获取并增加全局计数值
            // increment() 方法返回自增后的结果，如果结果为 null，则默认使用 0L
            long incremented = Optional.ofNullable(instance.opsForValue().increment(DISTRIBUTED_ID_GENERATOR_KEY))
                    .orElse(0L);

            // =============================
            // 设计说明：
            // 这里使用 incremented 作为分布式 ID 生成器的“节点标识位”。
            // 这样每次有新节点启动时，它会自动获得一个唯一编号，从而在分布式环境中生成不重复的 ID。
            // =============================

            // 定义节点编号的最大值为 32（即最多允许 32 个节点）
            int NODE_MAX = 32;

            // 如果节点编号超过最大限制，则重置为 0，并重置 Redis 中的计数器
            // 这是一种循环分配节点编号的简单实现方式
            if (incremented > NODE_MAX) {
                incremented = 0;
                instance.opsForValue().set(DISTRIBUTED_ID_GENERATOR_KEY, "0");
            }

            // 根据当前节点编号创建分布式 ID 生成器实例
            // incremented 表示节点 ID，用于区分不同服务实例生成的 ID
            DISTRIBUTED_ID_GENERATOR = new DistributedIdGenerator(incremented);
        } finally {
            // 释放分布式锁，防止死锁
            lock.unlock();
        }
    }
}
