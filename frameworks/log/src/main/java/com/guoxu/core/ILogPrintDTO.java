package com.guoxu.core;

import lombok.Data;

/**
 * ILogPrintDTO
 *
 * @author 执笔画棠
 * @date 2025/11/07 22:40
 **/
@Data
public class ILogPrintDTO {
    /**
     * 开始时间
     */
    private String beginTime;

    /**
     * 请求入参
     */
    private Object[] inputParams;

    /**
     * 返回参数
     */
    private Object outputParams;
}
