package com.guoxu.payservice.service.payid;

import com.guoxu.DistributedCache;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * PayIdGeneratorManager
 *
 * @author 执笔画棠
 * @date 2025/11/11 18:34
 **/
@Component
@RequiredArgsConstructor
// 支付ID生成器管理类（final修饰表示该类不可被继承），实现InitializingBean接口以在Bean初始化后执行初始化逻辑
public final class PayIdGeneratorManager implements InitializingBean {
    // Redisson客户端实例（用于分布式锁操作，保证分布式环境下初始化的线程安全）
    private final RedissonClient redissonClient;
    // 分布式缓存实例（用于存储分布式ID生成器的配置信息，如节点ID计数器）
    private final DistributedCache distributedCache;
    // 静态的分布式ID生成器实例（全局共享，用于生成唯一ID）
    private static DistributedIdGenerator DISTRIBUTED_ID_GENERATOR;

    /**
     * 生成支付全局唯一流水号
     *
     * @param orderSn 订单号（作为辅助信息，用于拼接流水号）
     * @return 支付流水号（分布式ID + 订单号后6位组成）
     */
    public static String generateId(String orderSn) {
        // 调用分布式ID生成器生成基础ID，拼接订单号的后6位（增强流水号的业务关联性）
        return DISTRIBUTED_ID_GENERATOR.generateId() + orderSn.substring(orderSn.length() - 6);
    }

    /**
     * InitializingBean接口方法，在Bean属性初始化完成后执行（用于初始化分布式ID生成器）
     * 作用：在分布式环境下为当前服务实例分配唯一的节点ID，保证ID生成器的唯一性
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        // 分布式锁的键（用于控制多实例并发初始化时的节点ID分配，避免冲突）
        String LOCK_KEY = "distributed_pay_id_generator_lock_key";
        // 通过Redisson获取分布式锁实例
        RLock lock = redissonClient.getLock(LOCK_KEY);
        // 加锁（阻塞等待，直到获取锁，保证同一时间只有一个实例执行节点ID分配逻辑）
        lock.lock();
        try {
            // 从分布式缓存中获取StringRedisTemplate实例（实际操作Redis的模板类）
            StringRedisTemplate instance = (StringRedisTemplate) distributedCache.getInstance();
            // 分布式ID生成器配置的缓存键（用于存储节点ID的自增计数器）
            String DISTRIBUTED_ID_GENERATOR_KEY = "distributed_pay_id_generator_config";
            // 原子递增缓存中的计数器值（获取当前实例的节点ID），若为null则默认0
            long incremented = Optional.ofNullable(instance.opsForValue().increment(DISTRIBUTED_ID_GENERATOR_KEY))
                    .orElse(0L);
            // 注意：这里是分库分表基因法的实现思路，节点ID最大为32（对应之前DistributedIdGenerator的NODE_BITS=5，2^5=32）
            // 补充：参考TB等平台的订单号设计，实际可能由全局服务生成，此处为应用内简化实现
            int NODE_MAX = 32;
            // 若计数器超过最大节点数，重置为0并更新缓存
            if (incremented > NODE_MAX) {
                incremented = 0;
                instance.opsForValue().set(DISTRIBUTED_ID_GENERATOR_KEY, "0");
            }
            // 初始化分布式ID生成器实例（使用当前分配的节点ID）
            DISTRIBUTED_ID_GENERATOR = new DistributedIdGenerator(incremented);
        } finally {
            // 释放分布式锁（无论初始化成功与否，都必须释放锁，避免死锁）
            lock.unlock();
        }
    }
}
