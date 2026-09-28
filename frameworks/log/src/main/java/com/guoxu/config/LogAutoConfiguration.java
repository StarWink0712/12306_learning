package com.guoxu.config;

import org.springframework.context.annotation.Bean;

/**
 * LogAutoConfiguration
 *
 * @author 执笔画棠
 * @date 2025/11/07 22:36
 **/
public class LogAutoConfiguration {
    /**
     * {@link ILog} 日志打印 AOP 切面
     */
    @Bean
    public ILogPrintAspect iLogPrintAspect() {
        return new ILogPrintAspect();
    }
}
