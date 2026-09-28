package com.guoxu.core;

/**
 * /**
 *  * 缓存加载器
 *  * 提供了从缓存中加载数据的功能，允许根据指定的参数加载缓存数据。
 *  */
@FunctionalInterface
public interface CacheLoader<T> {

    T load();
}