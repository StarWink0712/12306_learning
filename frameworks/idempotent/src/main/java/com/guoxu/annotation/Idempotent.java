package com.guoxu.annotation;

import com.guoxu.enums.IdempotentSceneEnum;
import com.guoxu.enums.IdempotentTypeEnum;

import java.lang.annotation.*;

/**
 * Idempotent
 * 幂等注解
 *
 * @author 执笔画棠
 * @version 2025/11/06 20:33
 **/
@Target({ElementType.TYPE,ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Idempotent {

    //幂等key
    String key() default "";

    //触发幂等失败逻辑时，返回的错误信息提示
    String message() default "您操作太快，请稍后重试";

    //验证幂等类型，支持多种方式
    IdempotentTypeEnum type()default IdempotentTypeEnum.PARAM;

    IdempotentSceneEnum scene() default IdempotentSceneEnum.RESTAPI;

    /**
     * 设置防重令牌 Key 前缀，MQ 幂等去重可选设置
     * {@link IdempotentSceneEnum#MQ} and {@link IdempotentTypeEnum#SPEL} 时生效
     */
    String uniqueKeyPrefix() default "";

    /**
     * 设置防重令牌 Key 过期时间，单位秒，默认 1 小时，MQ 幂等去重可选设置
     * {@link IdempotentSceneEnum#MQ} and {@link IdempotentTypeEnum#SPEL} 时生效
     */
    long keyTimeout() default 3600L;
}