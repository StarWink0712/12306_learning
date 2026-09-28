package com.guoxu.core.spel;

/**
 * IdempotentSpELByMQExecuteHandler
 *
 * @author 执笔画棠
 * @date 2025/11/06 20:18
 **/

import org.springframework.core.io.ClassPathResource;

import com.alibaba.ttl.threadpool.agent.internal.javassist.bytecode.SignatureAttribute;
import com.guoxu.DistributedCache;
import com.guoxu.annotation.Idempotent;
import com.guoxu.core.*;
import com.guoxu.enums.IdempotentMQConsumeStatusEnum;
import com.guoxu.toolkit.LogUtil;
import com.guoxu.toolkit.SpELUtil;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.aspectj.lang.reflect.MethodSignature;
import org.aspectj.lang.ProceedingJoinPoint;
import org.springframework.core.io.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 基于SpEL表达式的消息队列（MQ）幂等执行处理器
 * 继承AbstractIdempotentExecuteHandler：复用抽象类中幂等处理的基础流程（如参数包装、执行调度等）
 * 实现IdempotentSpELService：标记为SpEL表达式类型的幂等处理器，遵循相关接口规范
 * 声明为final类，禁止被继承（具体处理器实现通常无需扩展）
 */
@RequiredArgsConstructor
public class IdempotentSpELByMQExecuteHandler extends AbstractIdempotentExecuteHandler
        implements IdempotentSpELService{
    /**
     * 常量：默认超时时间（单位：秒），用于设置分布式缓存中键的过期时间
     */
    private final static int TIMEOUT = 600;

    /**
     * 常量：在幂等上下文中存储当前幂等参数包装器的键名
     * 用于后续在后置处理或异常处理中从上下文获取包装器对象
     */
    private final static String WRAPPER = "wrapper:spEL:MQ";

    /**
     * 常量：Lua脚本的路径（用于执行"不存在则设置并返回旧值"的原子操作）
     * Lua脚本保证分布式环境下操作的原子性，避免并发场景下的判断与设置分离导致的幂等失效
     */
    private final static String LUA_SCRIPT_SET_IF_ABSENT_AND_GET_PATH = "lua/set_if_absent_and_get.lua";

    /**
     * 分布式缓存组件（如Redis），用于存储MQ消息的消费状态，实现分布式环境下的幂等判断
     */
    private final DistributedCache distributedCache;

    /**
     * 重写父类方法，构建幂等参数包装器（IdempotentParamWrapper）
     * 核心逻辑：解析SpEL表达式生成MQ消息的唯一标识键，封装到包装器中供后续处理使用
     *
     * @param joinPoint AOP连接点，包含MQ消费方法的信息（如参数、签名等）
     * @return 构建好的幂等参数包装器，包含解析后的锁键（lockKey）、连接点等信息
     */
    @SneakyThrows // Lombok注解，自动捕获并包装受检异常为RuntimeException（简化异常处理）
    @Override
    protected IdempotentParamWrapper buildWrapper(ProceedingJoinPoint joinPoint) {
        // 从AOP连接点中获取目标方法上的@Idempotent注解（包含SpEL表达式等配置）
        Idempotent idempotent = IdempotentAspect.getIdempotent(joinPoint);
        // 解析SpEL表达式：根据注解中的key（SpEL表达式）、目标方法、方法参数，生成实际的唯一键字符串
        // （例如从MQ消息参数中提取消息ID、业务ID等，作为判断重复消费的标识）
        String key = (String) SpELUtil.parseKey(idempotent.key(),
                ((MethodSignature) joinPoint.getSignature()).getMethod(), joinPoint.getArgs());
        // 构建并返回幂等参数包装器：封装解析后的锁键、连接点等信息
        return IdempotentParamWrapper.builder().lockKey(key).joinPoint(joinPoint).build();
    }

    /**
     * 实现幂等核心处理逻辑：基于SpEL生成的唯一键判断MQ消息是否重复消费，通过分布式缓存记录消费状态
     *
     * @param wrapper 幂等参数包装器，包含解析后的唯一键、注解配置等信息
     */
    @Override
    public void handler(IdempotentParamWrapper wrapper) {
        // 生成分布式缓存的唯一键：注解中配置的唯一键前缀 + 解析后的SpEL键（确保键在分布式环境中唯一）
        String uniqueKey = wrapper.getIdempotent().uniqueKeyPrefix() + wrapper.getLockKey();
        // 调用setIfAbsentAndGet方法：执行Lua脚本，若键不存在则设置为"消费中"状态（超时时间600秒），并返回旧值
        // 若键已存在，则直接返回当前存储的状态（如"消费中"、"已消费"等）
        String absentAndGet = this.setIfAbsentAndGet(uniqueKey, IdempotentMQConsumeStatusEnum.CONSUMING.getCode(),
                TIMEOUT, TimeUnit.SECONDS);

        // 若返回值不为null，说明该消息已被处理过或正在处理（即重复消费）
        if (Objects.nonNull(absentAndGet)) {
            // 判断返回的状态是否为"错误状态"（如消费失败未清理的状态）
            boolean error = IdempotentMQConsumeStatusEnum.isError(absentAndGet);
            // 记录警告日志：提示消息重复消费，说明当前状态（错误状态则建议延迟消费，正常状态则说明已处理完成）
            LogUtil.getLog(wrapper.getJoinPoint()).warn("[{}] MQ repeated consumption, {}.", uniqueKey,
                    error ? "Wait for the client to delay consumption" : "Status is completed");
            // 抛出重复消费异常，携带错误状态标识（供上层判断是否需要重试）
            throw new RepeatConsumptionException(error);
        }
        // 若返回值为null（即首次消费），将包装器存入幂等上下文，供后续后置处理/异常处理使用
        IdempotentContext.put(WRAPPER, wrapper);
    }

    /**
     * 执行Lua脚本，实现"若键不存在则设置值并返回null，若键存在则返回当前值"的原子操作
     * 确保分布式环境下判断与设置的原子性，避免并发导致的重复消费误判
     *
     * @param key      分布式缓存的键（唯一标识MQ消息）
     * @param value    要设置的值（通常为消费状态，如"消费中"）
     * @param timeout  过期时间
     * @param timeUnit 时间单位
     * @return 若键已存在则返回当前值，否则返回null
     */
    public String setIfAbsentAndGet(String key, String value, long timeout, TimeUnit timeUnit) {
        // 创建Redis脚本对象（用于执行Lua脚本）
        DefaultRedisScript<String> redisScript = new DefaultRedisScript<>();
        // 加载类路径下的Lua脚本（路径为LUA_SCRIPT_SET_IF_ABSENT_AND_GET_PATH）
        ClassPathResource resource = new ClassPathResource(LUA_SCRIPT_SET_IF_ABSENT_AND_GET_PATH);
        redisScript.setScriptSource(new ResourceScriptSource(resource));
        // 设置脚本执行结果的类型为String
        redisScript.setResultType(String.class);

        // 将超时时间转换为毫秒（Lua脚本中通常使用毫秒作为时间单位）
        long millis = timeUnit.toMillis(timeout);
        // 执行Lua脚本：通过分布式缓存的StringRedisTemplate实例执行，参数为键列表、值、超时时间（毫秒）
        return ((StringRedisTemplate) distributedCache.getInstance()).execute(redisScript, List.of(key), value,
                String.valueOf(millis));
    }

    /**
     * 异常处理方法：在MQ消息消费抛出异常时调用，清理分布式缓存中的键
     * 确保异常场景下，消息可以被重新消费（避免因"消费中"状态残留导致消息丢失）
     */
    @Override
    public void exceptionProcessing() {
        // 从幂等上下文中获取之前存储的幂等参数包装器
        IdempotentParamWrapper wrapper = (IdempotentParamWrapper) IdempotentContext.getKey(WRAPPER);
        if (wrapper != null) {
            // 从包装器中获取@Idempotent注解和唯一键
            Idempotent idempotent = wrapper.getIdempotent();
            String uniqueKey = idempotent.uniqueKeyPrefix() + wrapper.getLockKey();
            try {
                // 删除分布式缓存中的键（清除"消费中"状态，允许消息重试）
                distributedCache.delete(uniqueKey);
            } catch (Throwable ex) {
                // 记录删除失败的错误日志（便于排查问题）
                LogUtil.getLog(wrapper.getJoinPoint()).error("[{}] Failed to del MQ anti-heavy token.", uniqueKey);
            }
        }
    }

    /**
     * 后置处理方法：在MQ消息消费成功后调用，更新分布式缓存中的状态为"已消费"
     * 确保后续重复消息可以识别出"已消费"状态，避免重复处理
     */
    @Override
    public void postProcessing() {
        // 从幂等上下文中获取之前存储的幂等参数包装器
        IdempotentParamWrapper wrapper = (IdempotentParamWrapper) IdempotentContext.getKey(WRAPPER);
        if (wrapper != null) {
            // 从包装器中获取@Idempotent注解和唯一键
            Idempotent idempotent = wrapper.getIdempotent();
            String uniqueKey = idempotent.uniqueKeyPrefix() + wrapper.getLockKey();
            try {
                // 更新缓存：将键的值设置为"已消费"，并使用注解中配置的超时时间（避免缓存永久存储）
                distributedCache.put(uniqueKey, IdempotentMQConsumeStatusEnum.CONSUMED.getCode(),
                        idempotent.keyTimeout(), TimeUnit.SECONDS);
            } catch (Throwable ex) {
                // 记录更新失败的错误日志（便于排查问题）
                LogUtil.getLog(wrapper.getJoinPoint()).error("[{}] Failed to set MQ anti-heavy token.", uniqueKey);
            }
        }
    }
}
