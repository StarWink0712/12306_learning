package com.guoxu.threadpool.proxty;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicLong;

/**
 * RejectedProxyInvocationHandler
 * 用于在线程池拒绝策略执行时，添加额外的逻辑，比如：拒绝任务报警或加入延迟队列重复放入等逻辑。
 * InvocationHandler 是 Java 动态代理的基础，主要用于在不修改目标对象代码的前提下，为方法添加通用横切逻辑
 * 当我们通过动态代理对象调用任意方法时，这个调用不会直接执行目标对象的方法，
 * 而是会被自动转发到 InvocationHandler 实现类的 invoke 方法中。
 * 开发者可以在 invoke 方法中编写通用逻辑（如 “前置处理”“后置处理”），再
 * 通过反射调用目标对象的原始方法，从而实现对目标方法的 “增强”。
 * @author 执笔画棠
 * @date 2025/11/04 17:23
 **/
@Slf4j
@AllArgsConstructor
public class RejectedProxyInvocationHandler implements InvocationHandler {

    /**
     * 被代理对象
     */
    private final Object target;

    /**
     * 拒绝策略执行次数
     */
    private final AtomicLong rejectCount;

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        rejectCount.incrementAndGet();

        try{
            log.error("线程池执行拒绝策略，检查原因");
            return method.invoke(target, args);
        }catch (InvocationTargetException ex){
            throw ex.getCause();
        }
    }
}
