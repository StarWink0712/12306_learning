package com.guoxu;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.security.PublicKey;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Singleton 单例容器
 *
 * @author 执笔画棠
 * @date 2025/11/04 19:21
 **/
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Singleton {
    //单例对象容器,用线程安全的集合存储单例对象
    private static final ConcurrentHashMap<String , Object> SINGLE_OBJECT_POOL=new ConcurrentHashMap<>();

    //根据key获取单例对象
    public static <T> T get(String key){
        Object result=SINGLE_OBJECT_POOL.get(key);
        return  result==null?null:(T) result;
    }

    public static <T> T get(String key, Supplier<T> supplier){
        Object result=SINGLE_OBJECT_POOL.get(key);
        if(result==null && (result=supplier.get())!=null){
            //如果集合里面没有这个单例对象的话，就用supplier创建一个单例对象放集合里面
            //同时返回
            SINGLE_OBJECT_POOL.put(key,result);
        }
        return  result==null?null:(T) result;
    }

    //对象放入容器
    public static void put(Object o){
        put(o.getClass().getName(),o);
    }

    public static void put(String key,Object o){
        SINGLE_OBJECT_POOL.put(key,o);
    }
}
