package com.guoxu.enums;

/**
 * IdempotentSceneEnum
 * 幂等验证场景枚举
 * @author 执笔画棠
 * @version 2025/11/06 20:41
 **/
public enum IdempotentSceneEnum {
    /**
     * 基于 RestAPI 场景验证
     */
    RESTAPI,

    /**
     * 基于 MQ 场景验证
     */
    MQ
}