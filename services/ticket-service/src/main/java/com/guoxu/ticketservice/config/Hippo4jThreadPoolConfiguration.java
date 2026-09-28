package com.guoxu.ticketservice.config;

import cn.hippo4j.common.executor.support.BlockingQueueTypeEnum;
import cn.hippo4j.core.executor.DynamicThreadPool;
import cn.hippo4j.core.executor.support.ThreadPoolBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Hippo4jThreadPoolConfiguration 动态线程池配置
 *
 * @author 执笔画棠
 * @date 2025/11/12 20:16
 **/
@Configuration
public class Hippo4jThreadPoolConfiguration {

    /**
     * 分配一个用户购买不同类型车票的线程池
     * <a href="https://nageoffer.com/12306/question">线程池参数如何设置</>
     */
    @Bean
    @DynamicThreadPool
    public ThreadPoolExecutor selectSeatThreadPoolExecutor() {
        String threadPoolId = "select-seat-thread-pool-executor";
        return ThreadPoolBuilder.builder()
                .threadPoolId(threadPoolId)
                .threadFactory(threadPoolId)
                .workQueue(BlockingQueueTypeEnum.SYNCHRONOUS_QUEUE)
                .corePoolSize(24)
                .maximumPoolSize(36)
                .allowCoreThreadTimeOut(true)
                .keepAliveTime(60, TimeUnit.MINUTES)
                .rejected(new ThreadPoolExecutor.CallerRunsPolicy())
                .dynamicPool()
                .build();
    }
}
