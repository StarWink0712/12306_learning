package com.guoxu;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.NonNull;

import java.util.Collection;

/**
 * Cache 缓存接口，定义了缓存的基本方法
 *
 * @author 执笔画棠
 * @version 2025/11/05 19:15
 **/
public interface Cache {

    //获取缓存
    <T> T get(@NotBlank  String key, Class<T> clazz);

    //放入缓存
    void put(@NotBlank String ket,Object value);

    //如果keys全不存在则新增，返回true，反之则false
    Boolean putIfAllAbsent(@NonNull Collection<String> keys);

    //删除缓存
    Boolean delete(@NotBlank String key);

    //删除keys，返回删除数量
    Long delete(@NotNull Collection<String> keys);

    //判断key是否存在
    Boolean hasKey(@NotBlank String key);

    //获取缓存组件实例
    Object getInstance();
}