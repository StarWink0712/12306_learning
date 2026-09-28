package com.guoxu.core;

/**
 * CacheGetFilter 缓存过滤器
 *提供了对缓存获取操作进行过滤的功能，允许根据特定条件来决定是否从缓存中获取数据。
 * @author 执笔画棠
 * @version 2025/11/05 19:39
 **/
@FunctionalInterface
public interface CacheGetFilter <T>{
    /**
     * 缓存过滤
     *
     * @param param 输出参数
     * @return {@code true} 如果输入参数匹配，否则 {@link Boolean#TRUE}
     */
    boolean filter(T param);
}