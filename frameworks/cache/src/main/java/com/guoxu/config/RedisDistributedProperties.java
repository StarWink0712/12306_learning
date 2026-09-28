package com.guoxu.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.concurrent.TimeUnit;

/**
 * RedisDistributedProperties
 *
 * @author 执笔画棠
 * @date 2025/11/05 19:53
 **/
@Data
@ConfigurationProperties(prefix = RedisDistributedProperties.PREFIX)
public class RedisDistributedProperties {
    public static final String PREFIX = "framework.cache.redis";

    /**
     * Key 前缀
     */
    private String prefix = "";

    /**
     * Key 前缀字符集
     */
    private String prefixCharset = "UTF-8";

    /**
     * 默认超时时间
     */
    private Long valueTimeout = 30000L;

    /**
     * 时间单位
     */
    private TimeUnit valueTimeUnit = TimeUnit.MILLISECONDS;
}
