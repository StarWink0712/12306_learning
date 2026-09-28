package com.guoxu.init;

import org.springframework.context.ApplicationEvent;

/**
 * ApplicationInitializingEvent 应用初始化事件
 *
 * @author 执笔画棠
 * @date 2025/11/04 20:30
 **/
public class ApplicationInitializingEvent extends ApplicationEvent {

    /**
     * 构造方法
     *
     * @param source 事件源
     */
    public ApplicationInitializingEvent(Object source) {
        super(source);
    }
}
