package com.guoxu.exception;

import com.guoxu.errorcode.IErrorCode;
import lombok.Getter;
import org.springframework.util.StringUtils;

import java.util.Optional;

@Getter
// 声明为抽象类，不能直接实例化，需要子类继承
public abstract class AbstractException extends RuntimeException {

    public final String errorCode;

    public final String errorMessage;

    // 构造方法
    public AbstractException(String message, Throwable throwable, IErrorCode errorCode) {
        super(message, throwable);
        this.errorCode = errorCode.code();
        this.errorMessage = Optional.ofNullable(StringUtils.hasLength(message) ? message : null)
                .orElse(errorCode.message());
    }
}
