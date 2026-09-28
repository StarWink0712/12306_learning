package com.guoxu.initialize;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.guoxu.config.WebAutoConfiguration.INITIALIZE_PATH;

/**
 * InitializeDispatcherServletController
 *
 * @author 执笔画棠
 * @date 2025/11/05 20:21
 **/
@Slf4j(topic = "Initialize DispatcherServlet")
@RestController
public final class InitializeDispatcherServletController {

    //接收自请求，完成应用程序的初始化，这样第一个请求的用户就不需要再多等几秒钟
    @GetMapping(INITIALIZE_PATH)
    public void initializeDispatcherServlet() {
        log.info("Initialized the dispatcherServlet to improve the first response time of the interface...");
    }
}
