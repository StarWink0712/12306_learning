package com.guoxu.config;

import com.guoxu.chain.AbstractChainContext;
import com.guoxu.strategy.AbstractStrategyChoose;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * DesignPatternAutoConfiguration 设计模式自动装配
 *
 * @author 执笔画棠
 * @date 2025/11/06 19:12
 **/
@ImportAutoConfiguration(ApplicationBaseAutoConfiguration.class)
public class DesignPatternAutoConfiguration {

    /**
     * 策略模式选择器
     */
    @Bean
    public AbstractStrategyChoose abstractStrategyChoose() {
        return new AbstractStrategyChoose();
    }

    /**
     * 责任链上下文
     */
    @Bean
    public AbstractChainContext abstractChainContext() {
        return new AbstractChainContext();
    }
}
