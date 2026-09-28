package com.guoxu.init;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * ApplicationContentPostProcessor 应用初始化后置处理器
 *
 * @author 执笔画棠
 * @date 2025/11/04 20:29
 **/
//此注解可以自动生成构造函数，为应用上下文实例生成构造函数
@RequiredArgsConstructor
public class ApplicationContentPostProcessor implements ApplicationListener<ApplicationReadyEvent> {

    //应用上下文实例，用于发布初始化事件
    private final ApplicationContext applicationContext;

    private final AtomicBoolean executeOnce = new AtomicBoolean(false);

    /**
     * 监听ApplicationReadyEvent事件，在应用启动完成后执行初始化逻辑
     * 这个方法就是应用启动完成后会执行的方法，在方法内部写要实现的逻辑
     * @param event 应用启动完成后的事件对象
     */
    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if(!executeOnce.compareAndSet(false,true)){
            //如果执行标志为false，尝试将其设置为true，确保事件只执行一次
            return;
        }

        //发布应用初始化事件，通知其他组件应用已初始化完成，其他应用在接收到通知后
        //就可以进行其自己的初始化逻辑了，比如缓存预热等
        applicationContext.publishEvent(new ApplicationInitializingEvent( this));

    }
}
