package com.guoxu.core;

import com.guoxu.annotation.Idempotent;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.reflect.Method;

/**
 * IdempotentAspect
 *
 * @author 执笔画棠
 * @date 2025/11/06 20:16
 **/
public class IdempotentAspect {
    /**
     * 环绕增强方法，用于处理标记了@Idempotent注解的方法
     * 核心逻辑：在目标方法执行前后插入幂等性判断、重复请求拦截等逻辑
     *
     * @param joinPoint AOP连接点对象，包含目标方法的信息（如参数、签名等）
     * @return 目标方法的执行结果
     * @throws Throwable 目标方法执行过程中可能抛出的异常
     */
    @Around("@annotation(org.opengoofy.index12306.framework.starter.idempotent.annotation.Idempotent)")
    public Object idempotentHandler(ProceedingJoinPoint joinPoint) throws Throwable {
        // 从AOP连接点中获取目标方法上的@Idempotent注解实例，用于获取幂等配置（场景、类型等）
        Idempotent idempotent = getIdempotent(joinPoint);
        // 根据注解中指定的幂等场景（scene）和处理类型（type），通过工厂类获取对应的幂等执行处理器
        // 不同场景（如RESTAPI、MQ）和类型（如PARAM、TOKEN、SPEL）对应不同的处理器实现
        IdempotentExecuteHandler instance = IdempotentExecuteHandlerFactory.getInstance(idempotent.scene(),
                idempotent.type());
        // 声明变量存储目标方法的执行结果
        Object resultObj;
        try {
            // 调用幂等处理器的execute方法，执行幂等预处理逻辑（如生成幂等键、检查是否重复请求等）
            instance.execute(joinPoint, idempotent);
            // 执行目标业务方法（被@Idempotent注解标记的方法），并获取执行结果
            resultObj = joinPoint.proceed();
            // 调用幂等处理器的后置处理方法，执行幂等逻辑的收尾操作（如更新幂等状态、清理临时数据等）
            instance.postProcessing();
        } catch (RepeatConsumptionException ex) {
            /**
             * 捕获重复消费异常（RepeatConsumptionException），处理重复请求场景：
             * 1. 若异常标记为非错误（ex.getError()为false）：表示消息可能仍在处理中，结果未知
             * 此时返回null，方便消息队列（如RocketMQ）通过重试队列重新投递消息
             * 2. 若异常标记为错误（ex.getError()为true）：表示消息已处理成功
             * 此时直接抛出异常，阻止重复处理
             */
            if (!ex.getError()) {
                return null;
            }
            throw ex;
        } catch (Throwable ex) {
            // 捕获其他业务异常：表示客户端处理过程中发生错误
            // 调用幂等处理器的异常处理方法，删除幂等标识（允许消息队列重试投递）
            instance.exceptionProcessing();
            // 重新抛出异常，让上层处理业务错误
            throw ex;
        } finally {
            // 无论处理成功或失败，最终清理当前线程的幂等上下文（IdempotentContext）
            // 避免线程复用（如线程池）导致的上下文数据残留，防止内存泄漏
            IdempotentContext.clean();
        }
        // 返回目标方法的执行结果
        return resultObj;
    }

    /**
     * 从AOP连接点中获取目标方法上的@Idempotent注解
     *
     * @param joinPoint AOP连接点对象，包含目标方法信息
     * @return 目标方法上的@Idempotent注解实例
     * @throws NoSuchMethodException 若反射获取目标方法失败时抛出
     */
    public static Idempotent getIdempotent(ProceedingJoinPoint joinPoint) throws NoSuchMethodException {
        // 从连接点中获取方法签名（MethodSignature），包含方法名、参数类型等信息
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        // 通过反射获取目标类（被代理的原始类）中与签名匹配的具体方法
        // 参数：方法名（从签名获取）、方法参数类型（从签名的方法对象中获取）
        Method targetMethod = joinPoint.getTarget().getClass().getDeclaredMethod(methodSignature.getName(),
                methodSignature.getMethod().getParameterTypes());
        // 从目标方法上获取@Idempotent注解并返回
        return targetMethod.getAnnotation(Idempotent.class);
    }
}
