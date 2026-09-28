package com.guoxu;

import org.opengoofy.index12306.framework.starter.convention.errorcode.BaseErrorCode;
import org.opengoofy.index12306.framework.starter.convention.exception.AbstractException;
import org.opengoofy.index12306.framework.starter.convention.result.Result;

import java.util.Optional;

/**
 * Results
 * 作为一个统一的工厂，用来创建和返回标准格式的API响应对象 (Result<T>)。
 * 这个 `Result<T>` 类就是用来承载这种结构的Java对象。
 * 而 `Results` 工具类就是为了让我们能够用一种**更简单、更统一、
 * 更不容易出错**的方式来构建这些 `Result<T>` 对象
 * @author 执笔画棠
 * @date 2025/11/05 20:29
 **/
public class Results {
    /**
     * 构造成功响应
     */
    public static Result<Void> success() {
        return new Result<Void>()
                .setCode(Result.SUCCESS_CODE);
    }

    /**
     * 构造带返回数据的成功响应
     */
    public static <T> Result<T> success(T data) {
        return new Result<T>()
                .setCode(Result.SUCCESS_CODE)
                .setData(data);
    }

    /**
     * 构建服务端失败响应
     */
    protected static Result<Void> failure() {
        return new Result<Void>()
                .setCode(BaseErrorCode.SERVICE_ERROR.code())
                .setMessage(BaseErrorCode.SERVICE_ERROR.message());
    }

    /**
     * 通过 {@link AbstractException} 构建失败响应
     */
    protected static Result<Void> failure(AbstractException abstractException) {
        String errorCode = Optional.ofNullable(abstractException.getErrorCode())
                .orElse(BaseErrorCode.SERVICE_ERROR.code());
        String errorMessage = Optional.ofNullable(abstractException.getErrorMessage())
                .orElse(BaseErrorCode.SERVICE_ERROR.message());
        return new Result<Void>()
                .setCode(errorCode)
                .setMessage(errorMessage);
    }

    /**
     * 通过 errorCode、errorMessage 构建失败响应
     */
    protected static Result<Void> failure(String errorCode, String errorMessage) {
        return new Result<Void>()
                .setCode(errorCode)
                .setMessage(errorMessage);
    }
}
