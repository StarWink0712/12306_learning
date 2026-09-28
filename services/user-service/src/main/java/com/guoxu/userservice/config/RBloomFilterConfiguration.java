package com.guoxu.userservice.config;

import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RBloomFilterConfiguration
 * 布隆过滤器配置
 * @author 执笔画棠
 * @date 2025/11/18 16:03
 **/
@Configuration
@EnableConfigurationProperties(UserRegisterBloomFilterProperties.class)
public class RBloomFilterConfiguration {
    /**
     * 防止用户注册缓存穿透的布隆过滤器
     */
    @Bean
    public RBloomFilter<String> userRegisterCachePenetrationBloomFilter(RedissonClient redissonClient,
                                                                        UserRegisterBloomFilterProperties userRegisterBloomFilterProperties) {
        RBloomFilter<String> cachePenetrationBloomFilter = redissonClient
                .getBloomFilter(userRegisterBloomFilterProperties.getName());
        cachePenetrationBloomFilter.tryInit(userRegisterBloomFilterProperties.getExpectedInsertions(),
                userRegisterBloomFilterProperties.getFalseProbability());
        return cachePenetrationBloomFilter;
    }
}
