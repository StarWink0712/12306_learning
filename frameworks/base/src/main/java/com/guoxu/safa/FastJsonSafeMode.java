package com.guoxu.safa;

import org.springframework.beans.factory.InitializingBean;

/**
 * FastJsonSafeMode
 *
 * @author 执笔画棠
 * @date 2025/11/04 21:03
 **/
public class FastJsonSafeMode implements InitializingBean {

    /*
    InitializingBean 是 Spring 框架提供的一个接口。
    任何一个 Spring Bean 如果实现了这个接口，那么 Spring 在创建并完成该 Bean 的所有属性注入之后，
    会自动调用它的 afterPropertiesSet() 方法。
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        System.setProperty("fastjson2.parser.safeMode", "true");
    }
}
