package com.guoxu.core.param;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.alibaba.fastjson2.JSON;
import com.guoxu.core.AbstractIdempotentExecuteHandler;
import com.guoxu.core.IdempotentContext;
import com.guoxu.core.IdempotentParamWrapper;
import com.guoxu.exception.ClientException;
import core.UserContext;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * IdempotentParamExecuteHandler
 *
 * @author 执笔画棠
 * @date 2025/11/06 20:18
 **/
@RequiredArgsConstructor
/**
 * 基于请求参数的REST API幂等执行处理器
 * 继承AbstractIdempotentExecuteHandler：复用抽象类中幂等处理的基础流程（如参数包装、执行调度等）
 * 实现IdempotentParamService：标记为参数类型的幂等处理器，遵循相关接口规范
 * 声明为final类，禁止被继承（具体处理器实现通常无需扩展）
 */
public class IdempotentParamExecuteHandler extends AbstractIdempotentExecuteHandler
        implements IdempotentParamService{
    /**
     * Redisson客户端实例，用于操作分布式锁（基于Redis的分布式锁实现）
     * 用于在分布式环境下保证幂等性判断的原子性，防止并发场景下重复请求误判
     */
    private final RedissonClient redissonClient;

    /**
     * 常量：在幂等上下文中存储分布式锁的键名
     * 用于后续在后置处理或异常处理中从上下文获取锁对象并释放
     */
    private final static String LOCK = "lock:param:restAPI";

    /**
     * 重写父类方法，构建幂等参数包装器（IdempotentParamWrapper）
     * 核心逻辑：通过"请求路径+当前用户ID+请求参数MD5"生成唯一锁键，确保同一用户的相同请求唯一标识
     *
     * @param joinPoint AOP连接点，包含目标方法的信息（如参数、签名等）
     * @return 构建好的幂等参数包装器，包含唯一锁键、连接点等信息
     */
    @Override
    protected IdempotentParamWrapper buildWrapper(ProceedingJoinPoint joinPoint) {
        // 生成唯一锁键：拼接请求路径、当前用户ID、请求参数MD5值，确保锁键全局唯一
        String lockKey = String.format("idempotent:path:%s:currentUserId:%s:md5:%s",
                getServletPath(), // 获取当前请求的Servlet路径
                getCurrentUserId(), // 获取当前操作用户ID
                calcArgsMD5(joinPoint));// 计算请求参数的MD5值（参数不变则MD5不变）
        // 构建并返回幂等参数包装器，封装生成的锁键和连接点信息
        return IdempotentParamWrapper.builder().lockKey(lockKey).joinPoint(joinPoint).build();
    }

    /**
     * 私有方法：获取当前线程绑定的HTTP请求的Servlet路径
     * 作用：作为锁键的一部分，区分不同的接口请求（不同接口路径对应不同锁键）
     *
     * @return 当前请求的Servlet路径（如"/api/order/create"）
     */
    private String getServletPath() {
        // 从Spring的RequestContextHolder获取当前线程的请求属性（ServletRequestAttributes）
        ServletRequestAttributes sra = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        // 返回请求的Servlet路径
        return sra.getRequest().getServletPath();
    }

    /**
     * 私有方法：获取当前操作用户的ID
     * 作用：作为锁键的一部分，区分不同用户的请求（同一接口不同用户的相同参数不互斥）
     *
     * @return 当前登录用户的ID
     * @throws ClientException 若用户ID获取失败（未登录等场景），抛出客户端异常
     */
    private String getCurrentUserId() {
        // 从用户上下文（UserContext）中获取当前用户ID（UserContext通常存储登录态信息）
        String userId = UserContext.getUserId();
        // 若用户ID为空（未登录或上下文未存储），抛出客户端异常提示登录
        if (StrUtil.isBlank(userId)) {
            throw new ClientException("用户ID获取失败，请登录");
        }
        return userId;
    }

    /**
     * 私有方法：计算AOP连接点中目标方法参数的MD5值
     * 作用：作为锁键的一部分，区分同一用户、同一接口的不同参数请求（参数不同则MD5不同）
     *
     * @param joinPoint AOP连接点，包含目标方法的参数信息
     * @return 方法参数的MD5十六进制字符串（参数不变则MD5值不变）
     */
    private String calcArgsMD5(ProceedingJoinPoint joinPoint) {
        // 1. 将方法参数转为JSON字节数组（JSON序列化保证参数顺序一致，避免因参数顺序不同导致MD5不同）
        // 2. 通过DigestUtil工具类计算MD5值，并转为十六进制字符串
        return DigestUtil.md5Hex(JSON.toJSONBytes(joinPoint.getArgs()));
    }

    /**
     * 实现幂等核心处理逻辑：基于生成的唯一锁键加分布式锁，拦截重复请求
     *
     * @param wrapper 幂等参数包装器，包含唯一锁键、注解配置等信息
     */
    @Override
    public void handler(IdempotentParamWrapper wrapper) {
        // 从包装器中获取之前生成的唯一锁键
        String lockKey = wrapper.getLockKey();
        // 通过Redisson客户端获取该锁键对应的分布式锁对象
        RLock lock = redissonClient.getLock(lockKey);
        // 尝试获取分布式锁（非阻塞式：立即返回结果，获取失败则直接抛出异常）
        // 若获取失败，说明同一用户的相同请求正在处理或已处理，触发幂等拦截
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
     * 直接复用postProcessing方法的释放逻辑，避免代码重复
     */
    @Override
    public void exceptionProcessing() {
        // 调用后置处理方法释放锁，确保异常场景下锁资源也能被释放
        postProcessing();
    }
}
