package com.guoxu.chain;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

/**
 * AbstractChainHandler
 * 抽象业务责任链
 * T是一个泛型类型参数，代表该责任链处理器要处理的请求参数的类型
 *
 * @author 执笔画棠
 * @version 2025/11/06 18:47
 **/
public interface AbstractChainHandler<T> extends Ordered {

    //执行责任链逻辑
    //就是你实现接口时指定的那个具体类型，在运行时传入什么对象，handler 就接收什么类型的参数
    void handler(T requestParam);

    //责任链组件标识
    String mark();
}