package com.guoxu.core;

import com.guoxu.annotation.Idempotent;
import org.aspectj.lang.ProceedingJoinPoint;

/**
 * IdempotentExecuteHandler
 * 幂等执行处理器，负责处理幂等逻辑。
 * 项目有多种幂等执行处理器
 * 如param的，spel的，基于token的
 * 这些处理器实现该接口
 * @author 执笔画棠
 * @date 2025/11/06 20:17
 **/
public interface IdempotentExecuteHandler {


    /**
     * 幂等处理逻辑
     *
     * @param wrapper 幂等参数包装器
     */
    void handler(IdempotentParamWrapper wrapper);

    /**
     * 执行幂等处理逻辑
     *
     * @param joinPoint  AOP 方法处理
     * @param idempotent 幂等注解
     */
    void execute(ProceedingJoinPoint joinPoint, Idempotent idempotent);

    /**
     * 异常流程处理
     */
    default void exceptionProcessing() {

    }

    /**
     * 后置处理
     */
    default void postProcessing() {

    }
}

