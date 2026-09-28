package com.guoxu;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.opengoofy.index12306.framework.starter.convention.errorcode.BaseErrorCode;
import org.opengoofy.index12306.framework.starter.convention.exception.AbstractException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.opengoofy.index12306.framework.starter.convention.result.Result;
import org.springframework.util.StringUtils;
import java.util.Optional;

/**
 * GlobalExceptionHandler 全局异常处理器
 *
 * @author 执笔画棠
 * @date 2025/11/05 20:30
 **/
@Slf4j
@RestControllerAdvice
// 全局异常处理类，用于统一处理应用程序中抛出的各种异常
public class GlobalExceptionHandler {

    /**
     * 拦截参数验证异常 - 处理Spring MVC参数校验失败抛出的异常
     */
    @SneakyThrows  // Lombok注解，自动抛出受检异常，简化代码
    @ExceptionHandler(value = MethodArgumentNotValidException.class)  // 指定处理的方法参数验证异常类型
    public Result validExceptionHandler(HttpServletRequest request, MethodArgumentNotValidException ex) {
        // 获取绑定结果，包含字段验证错误信息
        BindingResult bindingResult = ex.getBindingResult();
        // 获取第一个字段错误（通常是最先遇到的验证错误）
        FieldError firstFieldError = CollectionUtil.getFirst(bindingResult.getFieldErrors());
        // 获取错误消息，如果存在字段错误则使用其默认消息，否则使用空字符串
        String exceptionStr = Optional.ofNullable(firstFieldError)
                .map(FieldError::getDefaultMessage)  // 从字段错误中提取默认错误消息
                .orElse(StrUtil.EMPTY);  // 如果字段错误为空则返回空字符串
        // 记录错误日志：请求方法、请求URL、异常信息
        log.error("[{}] {} [ex] {}", request.getMethod(), getUrl(request), exceptionStr);
        // 返回客户端错误响应，包含错误码和错误消息
        return Results.failure(BaseErrorCode.CLIENT_ERROR.code(), exceptionStr);
    }

    /**
     * 拦截应用内抛出的自定义异常 - 处理业务逻辑中主动抛出的异常
     */
    @ExceptionHandler(value = { AbstractException.class })  // 指定处理的自定义异常类型
    public Result abstractException(HttpServletRequest request, AbstractException ex) {
        // 检查异常是否有根本原因（cause）
        if (ex.getCause() != null) {
            // 如果有根本原因，记录更详细的错误日志，包含异常链
            log.error("[{}] {} [ex] {}", request.getMethod(), request.getRequestURL().toString(), ex.toString(),
                    ex.getCause());
            // 返回失败结果，封装异常信息
            return Results.failure(ex);
        }
        // 如果没有根本原因，只记录当前异常信息
        log.error("[{}] {} [ex] {}", request.getMethod(), request.getRequestURL().toString(), ex.toString());
        // 返回失败结果，封装异常信息
        return Results.failure(ex);
    }

    /**
     * 拦截未捕获异常 - 作为最后的异常捕获兜底，处理所有未被前面方法捕获的异常
     */
    @ExceptionHandler(value = Throwable.class)  // 指定处理所有Throwable及其子类异常
    public Result defaultErrorHandler(HttpServletRequest request, Throwable throwable) {
        // 记录未捕获异常的错误日志，包含完整的堆栈跟踪
        log.error("[{}] {} ", request.getMethod(), getUrl(request), throwable);
        // 返回通用的失败响应，不暴露具体异常细节给客户端（安全考虑）
        return Results.failure();
    }

    /**
     * 获取完整的请求URL，包括查询参数
     * @param request HTTP请求对象
     * @return 完整的URL字符串
     */
    private String getUrl(HttpServletRequest request) {
        // 检查查询字符串是否为空
        if (StringUtils.isEmpty(request.getQueryString())) {
            // 如果没有查询参数，直接返回请求URL
            return request.getRequestURL().toString();
        }
        // 如果有查询参数，将URL和查询参数拼接成完整URL
        return request.getRequestURL().toString() + "?" + request.getQueryString();
    }
}
