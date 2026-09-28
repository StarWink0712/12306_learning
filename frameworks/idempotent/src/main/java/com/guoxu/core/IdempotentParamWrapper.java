package com.guoxu.core;

import com.guoxu.annotation.Idempotent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.aspectj.lang.ProceedingJoinPoint;

/**
 * IdempotentParamWrapper
 * 幂等参数包装器，用于封装幂等相关的参数。
 * @author 执笔画棠
 * @date 2025/11/06 20:17
 **/
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
//这个注解的作用是开启链式调用
@Accessors(chain = true)
public final class IdempotentParamWrapper {

    /**
     * 幂等注解
     */
    private Idempotent idempotent;

    /**
     * AOP 处理连接点
     */
    private ProceedingJoinPoint joinPoint;


    private String lockKey;

}
