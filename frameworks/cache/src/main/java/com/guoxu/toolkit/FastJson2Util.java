package com.guoxu.toolkit;

import com.alibaba.fastjson2.util.ParameterizedTypeImpl;

import java.lang.reflect.Type;

/**
 * FastJson2Util
 *提供了对 FastJson2 序列化和反序列化的工具方法，包括构建参数化类型等
 * @author 执笔画棠
 * @date 2025/11/05 19:48
 **/
public final class FastJson2Util {
    /**
     * 构建参数化类型（泛型类型）的方法
     * 用于支持FastJson2在反序列化时处理复杂的泛型类型
     * 例如：List<String>, Map<String, Object> 等
     *
     * @param types 类型参数数组，最后一个元素是原始类型，前面的元素是泛型参数
     * @return 构建好的参数化类型
     */
    public static Type buildType(Type... types) {
        // 初始化参数化类型变量，用于在循环中保存中间结果
        ParameterizedTypeImpl beforeType = null;

        // 检查types数组是否有效（非空且至少有一个元素）
        if (types != null && types.length > 0) {
            // 如果只有一个类型参数，创建最简单的参数化类型
            if (types.length == 1) {
                // 创建参数化类型：原始类型为types[0]，泛型参数为null（表示无参泛型）
                return new ParameterizedTypeImpl(new Type[] { null }, null, types[0]);
            }

            // 从后往前遍历types数组（从最后一个元素到第二个元素）
            // 这是因为参数化类型是嵌套结构的，需要从最内层开始构建
            for (int i = types.length - 1; i > 0; i--) {
                // 创建参数化类型实例：
                // - 第一个参数：泛型参数数组，如果是第一次循环使用types[i]，否则使用之前构建的beforeType
                // - 第二个参数：所有者类型（通常为null）
                // - 第三个参数：原始类型（types[i-1]）
                beforeType = new ParameterizedTypeImpl(
                        new Type[] {
                                // 如果是第一次循环，使用当前类型的原始类型
                                // 否则使用之前已经构建好的参数化类型作为泛型参数
                                beforeType == null ? types[i] : beforeType
                        },
                        null,  // 所有者类型，通常为null
                        types[i - 1]  // 原始类型（如List、Map等）
                );
            }
        }

        // 返回最终构建好的参数化类型
        return beforeType;
    }
}
