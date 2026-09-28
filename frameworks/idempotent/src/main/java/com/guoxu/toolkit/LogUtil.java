package com.guoxu.toolkit;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * LogUtil
 *
 * @author 执笔画棠
 * @date 2025/11/07 18:47
 **/
public class LogUtil {
    /**
     * 获取 Logger
     */
    public static Logger getLog(ProceedingJoinPoint joinPoint) {
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        return LoggerFactory.getLogger(methodSignature.getDeclaringType());
    }
}
