package com.guoxu.core;

/**
 * IdGenerator
 * 分布式id生成器接口
 *
 * @author 执笔画棠
 * @version 2025/11/06 16:39
 **/
public interface IdGenerator {

    //default是默认方法，如果不实现接口的这个方法，就会使用默认方法
    default long nextId(){
        return 0L;
    }

    default String nextIdStr(){
        return "";
    }
}