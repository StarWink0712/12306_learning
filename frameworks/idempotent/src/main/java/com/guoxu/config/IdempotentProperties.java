package com.guoxu.config;

import lombok.Data;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * IdempotentProperties
 *
 * @author 执笔画棠
 * @date 2025/11/06 20:19
 **/
@Data
@ConfigurationProperties(prefix = IdempotentProperties.PREFIX)
public class IdempotentProperties {
    public static final String PREFIX = "framework.idempotent.token";

    /**
     * Token 幂等 Key 前缀
     */
    private String prefix;

    /**
     * Token 申请后过期时间
     * 单位默认毫秒
     * 随着分布式缓存过期时间单位
     */
    private Long timeout;
}
