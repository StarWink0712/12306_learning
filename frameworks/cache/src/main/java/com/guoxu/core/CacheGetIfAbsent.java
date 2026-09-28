package com.guoxu.core;

/**
 * CacheGetIfAbsent
 *提供了在缓存查询结果为空时执行自定义逻辑的功能
 * @author 执笔画棠
 * @version 2025/11/05 19:46
 **/
@FunctionalInterface
public interface CacheGetIfAbsent<T> {
    /**
     * 如果查询结果为空，执行逻辑
     */
    void execute(T param);
}