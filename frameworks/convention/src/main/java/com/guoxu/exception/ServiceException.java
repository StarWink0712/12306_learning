package com.guoxu.exception;

import com.guoxu.errorcode.BaseErrorCode;
import com.guoxu.errorcode.IErrorCode;

import java.util.Optional;

/**
 * ServiceException 服务端异常
 *
 * @author 执笔画棠
 * @date 2025/11/05 21:32
 **/
public class ServiceException extends AbstractException{
    public ServiceException(String message) {
        this(message, null, BaseErrorCode.SERVICE_ERROR);
    }

    public ServiceException(IErrorCode errorCode) {
        this(null, errorCode);
    }

    public ServiceException(String message, IErrorCode errorCode) {
        this(message, null, errorCode);
    }

    public ServiceException(String message, Throwable throwable, IErrorCode errorCode) {
        super(Optional.ofNullable(message).orElse(errorCode.message()), throwable, errorCode);
    }

    @Override
    public String toString() {
        return "ServiceException{" +
                "code='" + errorCode + "'," +
                "message='" + errorMessage + "'" +
                '}';
    }
}
