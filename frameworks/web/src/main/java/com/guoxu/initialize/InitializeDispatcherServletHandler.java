package com.guoxu.initialize;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

import static com.guoxu.config.WebAutoConfiguration.INITIALIZE_PATH;

/**
 * InitializeDispatcherServletHandler
 *
 * @author 执笔画棠
 * @date 2025/11/05 20:39
 **/
@RequiredArgsConstructor
public class InitializeDispatcherServletHandler implements CommandLineRunner {
    private final RestTemplate restTemplate;

    private final ConfigurableEnvironment configurableEnvironment;
    //这个bean在应用启动时会被调用，run方法自动执行，发送初始化请求，先获取自身的端口
    //本机127.0.0.1地址8080端口，发送一个空的get请求，完成应用程序的初始化
    @Override
    public void run(String... args) throws Exception {
        // 发送初始化请求
        String url = String.format("http://127.0.0.1:%s%s",
                // 获取应用端口
                configurableEnvironment.getProperty("server.port", "8080")
                        + configurableEnvironment.getProperty("server.servlet.context-path", ""),
                INITIALIZE_PATH);
        try {
            // 发送GET请求
            restTemplate.execute(url, HttpMethod.GET, null, null);
        } catch (Throwable ignored) {
        }
    }
}
