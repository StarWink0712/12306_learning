package com.guoxu.ticketservice;

import cn.hippo4j.core.enable.EnableDynamicThreadPool;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableDynamicThreadPool//用于启用动态线程池功能
@MapperScan("org.opengoofy.index12306.biz.ticketservice.dao.mapper")
@EnableFeignClients("org.opengoofy.index12306.biz.ticketservice.remote")
public class TicketServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TicketServiceApplication.class, args);
    }

}
