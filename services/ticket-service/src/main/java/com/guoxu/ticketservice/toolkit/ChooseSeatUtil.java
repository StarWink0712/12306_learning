package com.guoxu.ticketservice.toolkit;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * ChooseSeatUtil
 *
 * @author 执笔画棠
 * @date 2025/11/12 22:33
 **/
// 定义一个不可继承的最终类ChooseSeatUtil，用于处理选座相关的工具方法
public final class ChooseSeatUtil {

    // 定义常量，表示商务座类型
    public static final String TRAIN_BUSINESS = "TRAIN_BUSINESS";
    // 定义常量，表示一等座类型
    public static final String TRAIN_FIRST = "TRAIN_FIRST";
    // 定义常量，表示二等座类型
    public static final String TRAIN_SECOND = "TRAIN_SECOND";

    // 定义一个接口StrPool，用于存储座位位置标识的常量
    interface StrPool {
        // 座位位置标识A
        String A = "A";
        // 座位位置标识B
        String B = "B";
        // 座位位置标识C
        String C = "C";
        // 座位位置标识D
        String D = "D";
        // 座位位置标识F
        String F = "F";
    }

    /**
     * 选座座位分类转换方法
     *
     * @param mark           座位类别标识，例如TRAIN_BUSINESS、TRAIN_FIRST、TRAIN_SECOND
     * @param chooseSeatList 选座座位集合，包含座位位置信息，如 ["3A", "5C"] 等格式
     * @return 选择座位位置的HashMap，键为座位位置分类的索引，值为该分类下座位的数量
     */
    public static HashMap<Integer, Integer> convert(String mark, List<String> chooseSeatList) {
        // 创建一个HashMap用于存储转换后的选座信息，初始容量为8
        HashMap<Integer, Integer> actualChooseSeatMap = new HashMap<>(8);
        // 根据座位位置的首字母对选座座位集合进行分组
        Map<String, List<String>> chooseSeatMap = chooseSeatList
                .stream()
                .collect(Collectors.groupingBy(seat -> seat.substring(0, 1)));
        // 遍历分组后的选座信息
        chooseSeatMap.forEach((key, value) -> {
            // 根据座位类别标识进行不同的处理
            switch (mark) {
                case TRAIN_BUSINESS -> {
                    // 对于商务座，根据座位位置首字母确定索引并设置数量
                    switch (key) {
                        case StrPool.A -> actualChooseSeatMap.put(0, value.size());
                        case StrPool.C -> actualChooseSeatMap.put(1, value.size());
                        case StrPool.F -> actualChooseSeatMap.put(2, value.size());
                    }
                }
                case TRAIN_FIRST -> {
                    // 对于一等座，根据座位位置首字母确定索引并设置数量
                    switch (key) {
                        case StrPool.A -> actualChooseSeatMap.put(0, value.size());
                        case StrPool.C -> actualChooseSeatMap.put(1, value.size());
                        case StrPool.D -> actualChooseSeatMap.put(2, value.size());
                        case StrPool.F -> actualChooseSeatMap.put(3, value.size());
                    }
                }
                case TRAIN_SECOND -> {
                    // 对于二等座，根据座位位置首字母确定索引并设置数量
                    switch (key) {
                        case StrPool.A -> actualChooseSeatMap.put(0, value.size());
                        case StrPool.B -> actualChooseSeatMap.put(1, value.size());
                        case StrPool.C -> actualChooseSeatMap.put(2, value.size());
                        case StrPool.D -> actualChooseSeatMap.put(3, value.size());
                        case StrPool.F -> actualChooseSeatMap.put(4, value.size());
                    }
                }
            }
        });
        // 返回转换后的选座信息HashMap
        return actualChooseSeatMap;
    }
}