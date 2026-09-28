package com.guoxu.exception;

import com.guoxu.errorcode.BaseErrorCode;
import com.guoxu.errorcode.IErrorCode;

/**
 * ClientException 客户端异常
 *
 * @author 执笔画棠
 * @date 2025/11/05 21:31
 **/
public class ClientException extends AbstractException{
    public ClientException(IErrorCode errorCode) {
        this(null, null, errorCode);
    }

    public ClientException(String message) {
        this(message, null, BaseErrorCode.CLIENT_ERROR);
    }

    public ClientException(String message, IErrorCode errorCode) {
        this(message, null, errorCode);
    }

    public ClientException(String message, Throwable throwable, IErrorCode errorCode) {
        super(message, throwable, errorCode);
    }

    @Override
    public String toString() {
        return "ClientException{" +
                "code='" + errorCode + "'," +
                "message='" + errorMessage + "'" +
                '}';
    }
}
