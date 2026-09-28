package com.guoxu.core;


import com.baomidou.mybatisplus.core.toolkit.SystemClock;
import com.guoxu.annotation.ILog;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.assertj.core.util.DateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;
import com.alibaba.fastjson2.JSON;

import java.lang.reflect.Method;
import java.util.Optional;

import static org.apache.logging.log4j.message.MapMessage.MapFormat.JSON;

/**
 * ILogPrintAspect 日志打印aop切面
 *
 * @author 执笔画棠
 * @date 2025/11/07 22:36
 **/
@Aspect
public class ILogPrintAspect {

    /**
     * 环绕通知：打印类或方法上的 {@link ILog} 注解信息
     * 使用@Around注解拦截被@ILog注解标记的类或方法
     */
    @Around("@within(org.index12306.framework.starter.log.annotation.ILog) || @annotation(org.index12306.framework.starter.log.annotation.ILog)")
    public Object printMLog(ProceedingJoinPoint joinPoint) throws Throwable {
        // 记录方法开始执行的时间戳（使用高性能时钟）
        long startTime = SystemClock.now();
        // 获取方法签名信息，用于后续获取方法详细信息
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        // 获取当前类的Logger对象，用于日志输出
        Logger log = LoggerFactory.getLogger(methodSignature.getDeclaringType());
        // 获取格式化的当前时间字符串，用于记录开始时间
        String beginTime = DateUtil.now();
        // 定义方法返回值变量，初始为null
        Object result = null;
        try {
            // 执行目标方法，并获取返回值
            result = joinPoint.proceed();
        } finally {
            // 在finally块中确保日志一定会被打印，即使方法抛出异常

            // 通过反射获取目标方法的Method对象
            Method targetMethod = joinPoint.getTarget().getClass().getDeclaredMethod(methodSignature.getName(), methodSignature.getMethod().getParameterTypes());
            // 优先获取方法上的@ILog注解，如果方法上没有则获取类上的@ILog注解
            ILog logAnnotation = Optional.ofNullable(targetMethod.getAnnotation(ILog.class)).orElse(joinPoint.getTarget().getClass().getAnnotation(ILog.class));

            // 如果存在@ILog注解，则进行日志打印
            if (logAnnotation != null) {
                // 创建日志打印数据传输对象
                ILogPrintDTO logPrint = new ILogPrintDTO();
                // 设置方法开始执行的时间
                logPrint.setBeginTime(beginTime);

                // 如果注解配置需要记录输入参数
                if (logAnnotation.input()) {
                    // 构建输入参数信息并设置到DTO中
                    logPrint.setInputParams(buildInput(joinPoint));
                }

                // 如果注解配置需要记录输出结果
                if (logAnnotation.output()) {
                    // 设置方法返回值到DTO中
                    logPrint.setOutputParams(result);
                }

                // 初始化HTTP请求方法类型和URI变量
                String methodType = "", requestURI = "";
                try {
                    // 尝试从Spring的RequestContextHolder中获取当前请求的上下文信息
                    ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                    // 断言确保请求属性不为null
                    assert servletRequestAttributes != null;
                    // 获取HTTP请求方法（GET、POST等）
                    methodType = servletRequestAttributes.getRequest().getMethod();
                    // 获取请求的URI路径
                    requestURI = servletRequestAttributes.getRequest().getRequestURI();
                } catch (Exception ignored) {
                    // 忽略异常，如果不在Web环境或无法获取请求信息，则使用空字符串
                }

                // 打印完整的日志信息，包括请求方法、URI、执行时间和详细的参数信息
                log.info("[{}] {}, executeTime: {}ms, info: {}", methodType, requestURI, SystemClock.now() - startTime, JSON.toJSONString(logPrint));
            }
        }
        // 返回目标方法的执行结果
        return result;
    }

    /**
     * 构建输入参数信息，过滤掉不适合日志输出的参数类型
     * @param joinPoint 连接点对象，包含方法参数信息
     * @return 处理后的参数数组
     */
    private Object[] buildInput(ProceedingJoinPoint joinPoint) {
        // 获取原始的方法参数数组
        Object[] args = joinPoint.getArgs();
        // 创建新的参数数组用于日志输出
        Object[] printArgs = new Object[args.length];

        // 遍历所有参数
        for (int i = 0; i < args.length; i++) {
            // 如果参数是HttpServletRequest或HttpServletResponse类型，跳过不记录
            if ((args[i] instanceof HttpServletRequest) || args[i] instanceof HttpServletResponse) {
                continue;
            }
            // 如果参数是字节数组，用"byte array"代替，避免输出大量二进制数据
            if (args[i] instanceof byte[]) {
                printArgs[i] = "byte array";
            }
            // 如果参数是文件上传对象，用"file"代替，避免输出文件内容
            else if (args[i] instanceof MultipartFile) {
                printArgs[i] = "file";
            }
            // 其他类型的参数直接保留原值
            else {
                printArgs[i] = args[i];
            }
        }
        // 返回处理后的参数数组
        return printArgs;
    }
}