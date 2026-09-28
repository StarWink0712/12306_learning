package com.guoxu.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * ILog
 * 注解打印，可以标志在类和方法上
 * 标记在类上，类下所有方法都会打印；标记在方法上，仅打印标记方法；如果类或者方法上都有标记，以方法上注解为准
 * @author 执笔画棠
 * @version 2025/11/07 22:35
 **/
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ILog {
    /**
     * 入参打印
     *
     * @return 打印结果中是否包含入参，{@link Boolean#TRUE} 打印，{@link Boolean#FALSE} 不打印
     */
    boolean input() default true;

    /**
     * 出参打印
     *
     * @return 打印结果中是否包含出参，{@link Boolean#TRUE} 打印，{@link Boolean#FALSE} 不打印
     */
    boolean output() default true;
}