package com.guoxu.annotation;

import com.guoxu.enums.IdempotentSceneEnum;
import com.guoxu.enums.IdempotentTypeEnum;
import jdk.jfr.Percentage;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;

/**
 * MQIdempotent
 *
 * @author 执笔画棠
 * @version 2025/11/06 20:45
 **/
@Deprecated
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Idempotent(scene = IdempotentSceneEnum.MQ)
public @interface MQIdempotent {

    /**
     * {@link Idempotent#key} 的别名
     */
    @AliasFor(annotation = Idempotent.class, attribute = "key")
    String key() default "";

    /**
     * {@link Idempotent#type} 的别名
     */
    @AliasFor(annotation = Idempotent.class, attribute = "type")
    IdempotentTypeEnum type() default IdempotentTypeEnum.SPEL;
}