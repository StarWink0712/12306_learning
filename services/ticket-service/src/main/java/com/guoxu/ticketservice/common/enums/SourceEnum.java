package com.guoxu.ticketservice.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * SourceEnum 购票来源
 *
 * @author 执笔画棠
 * @version 2025/11/12 20:12
 **/
@RequiredArgsConstructor
public enum SourceEnum {
    /**
     * 互联网购票
     */
    INTERNET(0),

    /**
     * 线下窗口购票
     */
    OFFLINE(1);

    @Getter
    private final Integer code;
}