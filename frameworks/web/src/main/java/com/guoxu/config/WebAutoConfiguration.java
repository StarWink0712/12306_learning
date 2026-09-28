package com.guoxu.config;

import com.guoxu.GlobalExceptionHandler;
import com.guoxu.initialize.InitializeDispatcherServletController;
import com.guoxu.initialize.InitializeDispatcherServletHandler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * WebAutoConfiguration web组件自动装配
 *
 * @author 执笔画棠
 * @date 2025/11/05 20:18
 **/
// Web自动配置类 - 用于配置Web相关的Bean和组件
// 通常使用@Configuration注解，在Spring Boot启动时自动加载
public class WebAutoConfiguration {

    // 初始化DispatcherServlet的端点路径常量
    // 用于应用启动时预热DispatcherServlet，改善首次接口响应时间
    public final static String INITIALIZE_PATH = "/initialize/dispatcher-servlet";

    /**
     * 配置全局异常处理器Bean
     * @ConditionalOnMissingBean 表示只有当容器中不存在GlobalExceptionHandler类型的Bean时才创建
     * 这样可以允许用户自定义全局异常处理器来覆盖默认实现
     */
    @Bean
    @ConditionalOnMissingBean
    public GlobalExceptionHandler globalExceptionHandler() {
        // 创建并返回全局异常处理器实例
        return new GlobalExceptionHandler();
    }

    /**
     * 配置DispatcherServlet初始化控制器Bean
     * 该控制器提供了一个端点，用于在应用启动后预热DispatcherServlet
     * 避免首次请求时的冷启动延迟问题
     */
    @Bean
    public InitializeDispatcherServletController initializeDispatcherServletController() {
        // 创建并返回DispatcherServlet初始化控制器实例
        return new InitializeDispatcherServletController();
    }

    /**
     * 配置简单的RestTemplate Bean
     * RestTemplate是Spring提供的用于消费RESTful服务的模板类
     * @param factory HTTP请求工厂，用于配置RestTemplate的底层HTTP连接
     * @return 配置好的RestTemplate实例
     */
    @Bean
    public RestTemplate simpleRestTemplate(ClientHttpRequestFactory factory) {
        // 使用提供的HTTP请求工厂创建RestTemplate实例
        return new RestTemplate(factory);
    }

    /**
     * 配置HTTP客户端请求工厂Bean
     * 用于为RestTemplate提供底层的HTTP连接配置
     * 这里使用SimpleClientHttpRequestFactory实现
     */
    @Bean
    public ClientHttpRequestFactory simpleClientHttpRequestFactory() {
        // 创建简单的HTTP请求工厂实例
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        // 设置读取超时时间为5秒（从服务器读取数据的超时时间）
        factory.setReadTimeout(5000);
        // 设置连接超时时间为5秒（与服务器建立连接的超时时间）
        factory.setConnectTimeout(5000);
        // 返回配置好的请求工厂
        return factory;
    }

    /**
     * 配置DispatcherServlet初始化处理器Bean
     * 该处理器负责在应用启动后自动调用初始化端点，完成DispatcherServlet的预热
     * @param simpleRestTemplate 用于发送HTTP请求的RestTemplate实例
     * @param configurableEnvironment Spring环境配置，用于获取应用端口等配置信息
     * @return 配置好的初始化处理器实例
     */
    @Bean
    public InitializeDispatcherServletHandler initializeDispatcherServletHandler(RestTemplate simpleRestTemplate,
                                                                                 ConfigurableEnvironment configurableEnvironment) {
        // 创建并返回DispatcherServlet初始化处理器实例
        // 注入RestTemplate用于发送初始化请求，注入环境配置用于构建正确的URL
        return new InitializeDispatcherServletHandler(simpleRestTemplate, configurableEnvironment);
    }
}
