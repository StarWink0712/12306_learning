package com.guoxu.exception;

import com.guoxu.errorcode.BaseErrorCode;
import com.guoxu.errorcode.IErrorCode;

/**
 * RemoteException 远程服务调用异常
 *
 * @author 执笔画棠
 * @date 2025/11/05 21:32
 **/
public class RemoteException extends AbstractException{
    public RemoteException(String message) {
        this(message, null, BaseErrorCode.REMOTE_ERROR);
    }

    public RemoteException(String message, IErrorCode errorCode) {
        this(message, null, errorCode);
    }

    public RemoteException(String message, Throwable throwable, IErrorCode errorCode) {
        super(message, throwable, errorCode);
    }

    @Override
    public String toString() {
        return "RemoteException{" +
                "code='" + errorCode + "'," +
                "message='" + errorMessage + "'" +
                '}';
    }
}
