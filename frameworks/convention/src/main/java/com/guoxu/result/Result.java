package com.guoxu.result;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;

/**
 * Result
 *
 * @author 执笔画棠
 * @date 2025/11/05 21:16
 **/
@Data
// accessors 链式调用
@Accessors(chain = true)
public class Result<T> implements Serializable {

    // serial注解，用于序列化
    @Serial
    private static final long serialVersionUID = 5679018624309023727L;

    /**
     * 正确返回码
     */
    public static final String SUCCESS_CODE = "0";

    /**
     * 返回码
     */
    private String code;

    /**
     * 返回消息
     */
    private String message;

    /**
     * 响应数据
     */
    private T data;

    /**
     * 请求ID
     */
    private String requestId;

    public boolean isSuccess() {
        return SUCCESS_CODE.equals(code);
    }
}
