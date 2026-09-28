package com.guoxu.ticketservice.common.enums;

import cn.hutool.core.util.StrUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * CanalExecuteStrategyMarkEnum
 *
 * @author 执笔画棠
 * @version 2025/11/12 20:01
 **/
@RequiredArgsConstructor
public enum CanalExecuteStrategyMarkEnum {

    /**
     * 座位表
     */
    T_SEAT("t_seat", null),

    /**
     * 订单表
     */
    T_ORDER("t_order", "^t_order_([0-9]+|1[0-6])");

    // 实际表名
    @Getter
    private final String actualTable;

    // 模式匹配表名
    @Getter
    private final String patternMatchTable;

    public static boolean isPatternMatch(String tableName) {
        return Arrays.stream(CanalExecuteStrategyMarkEnum.values())
                .anyMatch(each -> StrUtil.isNotBlank(each.getPatternMatchTable())
                        && Pattern.compile(each.getPatternMatchTable()).matcher(tableName).matches());
    }

    public static String getPatternMatch(String tableName) {
        return Arrays.stream(CanalExecuteStrategyMarkEnum.values())
                .filter(each -> Objects.equals(tableName, each.getActualTable()))
                .findFirst()
                .map(CanalExecuteStrategyMarkEnum::getPatternMatchTable)
                .orElse(null);
    }
}