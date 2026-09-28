package com.guoxu.userservice.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * VerifyStatusEnum
 * 用户注册错误码枚举
 * @author 执笔画棠
 * @version 2025/11/18 16:02
 **/
@AllArgsConstructor
public enum VerifyStatusEnum {
    /**
     * 未审核
     */
    UNREVIEWED(0),

    /**
     * 已审核
     */
    REVIEWED(1);

    @Getter
    private final int code;
}