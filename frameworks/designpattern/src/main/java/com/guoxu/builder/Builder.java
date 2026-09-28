package com.guoxu.builder;

import java.io.Serializable;

/**
 * Builder
 * 模式抽象接口
 * 是构建者模式的基础接口定义，用于构建一个产品对象。
 *
 * @author 执笔画棠
 * @version 2025/11/04 15:38
 **/
public interface Builder<T> extends Serializable {
    /**
     * 构建方法
     * 用于构建一个产品对象
     * @return 产品对象
     */
    // 构建方法
    T build();
}