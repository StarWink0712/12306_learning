package com.guoxu.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.guoxu.handler.CustomIdGenerator;
import com.guoxu.handler.MyMetaObjectHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * MybatisPlusAutoConfiguration
 * 这是一个非常核心的 Spring Boot 自动配置类，其作用是为 MyBatis-Plus 框架配置和启用一系列高级功能。
 * 通过将这些配置以 @Bean 的形式集中管理，它极大地简化了框架的集成和使用，并使得项目的技术规范得以统一实施。
 * @author 执笔画棠
 * @date 2025/11/06 12:57
 **/
public class MybatisPlusAutoConfiguration {
    /**
     * 分页插件
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }

    /**
     * 元数据填充
     */
    @Bean
    public MyMetaObjectHandler myMetaObjectHandler() {
        return new MyMetaObjectHandler();
    }

    /**
     * 自定义雪花算法 ID 生成器
     */
    @Bean
    @Primary
    public IdentifierGenerator idGenerator() {
        return new CustomIdGenerator();
    }
}
