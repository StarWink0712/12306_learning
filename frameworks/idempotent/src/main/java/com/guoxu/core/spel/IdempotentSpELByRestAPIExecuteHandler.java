package com.guoxu.core.spel;

import com.guoxu.annotation.Idempotent;
import com.guoxu.core.AbstractIdempotentExecuteHandler;
import com.guoxu.core.IdempotentAspect;
import com.guoxu.core.IdempotentContext;
import com.guoxu.core.IdempotentParamWrapper;
import com.guoxu.exception.ClientException;
import com.guoxu.toolkit.SpELUtil;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

/**
 * IdempotentSpELByRestAPIExecuteHandler
 *
 * @author 执笔画棠
 * @date 2025/11/06 20:18
 **/
@RequiredArgsConstructor
public final class IdempotentSpELByRestAPIExecuteHandler extends AbstractIdempotentExecuteHandler
        implements IdempotentSpELService{

    /**
     * Redisson客户端实例，用于操作分布式锁（基于Redis的分布式锁实现）
     * 用于在分布式环境下保证幂等性判断的原子性（防止并发场景下的重复请求误判）
     */
    private final RedissonClient redissonClient;

    /**
     * 常量：在幂等上下文中存储分布式锁的键名
     * 用于后续在后置处理或异常处理中从上下文获取锁对象并释放
     */
    private final static String LOCK = "lock:spEL:restAPI";

    /**
     * 重写父类方法，构建幂等参数包装器（IdempotentParamWrapper）
     * 核心逻辑：解析SpEL表达式生成幂等键，封装到包装器中供后续处理使用
     *
     * @param joinPoint AOP连接点，包含目标方法的信息（如参数、签名等）
     * @return 构建好的幂等参数包装器，包含解析后的幂等键、连接点等信息
     */
    @SneakyThrows // Lombok注解，自动捕获并包装受检异常为RuntimeException（简化异常处理）
    @Override
    protected IdempotentParamWrapper buildWrapper(ProceedingJoinPoint joinPoint) {
        // 从AOP连接点中获取目标方法上的@Idempotent注解（包含SpEL表达式等配置）
        Idempotent idempotent = IdempotentAspect.getIdempotent(joinPoint);
        // 解析SpEL表达式：根据注解中的key（SpEL表达式）、目标方法、方法参数，生成实际的幂等键字符串
        // SpELUtil是SpEL表达式解析工具类，支持从方法参数中提取变量并替换表达式中的占位符
        String key = (String) SpELUtil.parseKey(idempotent.key(),
                ((MethodSignature) joinPoint.getSignature()).getMethod(), joinPoint.getArgs());
        // 构建并返回幂等参数包装器：封装解析后的锁键（lockKey）、连接点等信息
        return IdempotentParamWrapper.builder().lockKey(key).joinPoint(joinPoint).build();
    }

    /**
     * 实现幂等核心处理逻辑：基于SpEL生成的唯一键加分布式锁，防止重复请求
     *
     * @param wrapper 幂等参数包装器，包含解析后的幂等键、注解配置等信息
     */
    @Override
    public void handler(IdempotentParamWrapper wrapper) {
        // 生成分布式锁的唯一键：注解中配置的唯一键前缀 + 解析后的SpEL键（确保键在分布式环境中唯一）
        String uniqueKey = wrapper.getIdempotent().uniqueKeyPrefix() + wrapper.getLockKey();
        // 通过Redisson客户端获取该唯一键对应的分布式锁对象
        RLock lock = redissonClient.getLock(uniqueKey);
        // 尝试获取分布式锁（非阻塞式：立即返回结果，获取失败则直接抛出异常）
        // 若获取失败，说明有相同请求正在处理或已处理，触发幂等拦截
        if (!lock.tryLock()) {
            // 抛出客户端异常，异常消息使用注解中配置的自定义消息
            throw new ClientException(wrapper.getIdempotent().message());
        }
        // 若获取锁成功，将锁对象存入幂等上下文（IdempotentContext），供后续释放锁使用
        IdempotentContext.put(LOCK, lock);
    }

    /**
     * 后置处理方法：在目标方法执行成功后调用，释放分布式锁
     * 确保锁资源被正确释放，避免分布式环境下的死锁
     */
    @Override
    public void postProcessing() {
        RLock lock = null;
        try {
            // 从幂等上下文中获取之前存储的分布式锁对象
            lock = (RLock) IdempotentContext.getKey(LOCK);
        } finally {
            // 无论是否获取到锁对象，最终若锁不为null则释放锁
            if (lock != null) {
                lock.unlock();
            }
        }
    }

    /**
     * 异常处理方法：在目标方法执行抛出异常时调用，释放分布式锁
     * 确保异常场景下锁资源也能被释放，避免死锁
     */
    @Override
    public void exceptionProcessing() {
        RLock lock = null;
        try {
            // 从幂等上下文中获取之前存储的分布式锁对象
            lock = (RLock) IdempotentContext.getKey(LOCK);
        } finally {
            // 无论是否获取到锁对象，最终若锁不为null则释放锁
            if (lock != null) {
                lock.unlock();
            }
        }
    }
}
