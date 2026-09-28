package com.guoxu.config;

import com.guoxu.ApplicationContextHolder;
import com.guoxu.init.ApplicationContentPostProcessor;
import com.guoxu.safa.FastJsonSafeMode;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

/**
 * ApplicationBaseAutoConfiguration 应用基础自动装配类
 * 让spring容器启动后自动的把应用上下文持有者，安全模式，应用后置处理器自动注入到ioc容器中
 * @author 执笔画棠
 * @date 2025/11/04 20:20
 **/
public class ApplicationBaseAutoConfiguration {

    /*
     * 这个流程很简单，spring启动后，会自动把这些bean注入到ioc容器中
     所有应用启动后，会触发ApplicationContentPostProcessor的onApplicationEvent方法、
     在这个方法内部，应用上下文实例会发布事件初始化，实现了ApplicationInitializingEvent的应用
     就会收到通知，然后触发自己的初始化逻辑
     */
    @Bean
    // 此注解允许用户自定义实现来覆盖框架的默认行为。
    @ConditionalOnMissingBean
    public ApplicationContextHolder congoApplicationContextHolder() {
        // 初始化 ApplicationContextHolder 实例，将当前应用上下文设置为静态变量 CONTEXT。
        return new ApplicationContextHolder();
    }

    @Bean
    @ConditionalOnMissingBean
    public ApplicationContentPostProcessor congoApplicationContentPostProcessor(ApplicationContext applicationContext) {
        // 初始化 ApplicationContentPostProcessor 实例，将当前应用上下文设置为构造参数。
        return new ApplicationContentPostProcessor(applicationContext);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(value = "framework.fastjson.safa-mode", havingValue = "true")
    public FastJsonSafeMode congoFastJsonSafeMode() {
        // 初始化 FastJsonSafeMode 实例，开启 FastJson 安全模式。
        return new FastJsonSafeMode();
    }
}
