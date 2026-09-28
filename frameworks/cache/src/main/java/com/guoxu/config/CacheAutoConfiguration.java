package com.guoxu.config;

import com.guoxu.RedisKeySerializer;
import com.guoxu.StringRedisTemplateProxy;
import lombok.AllArgsConstructor;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * CacheAutoConfiguration
 *
 * @author 执笔画棠
 * @date 2025/11/05 20:02
 **/
@AllArgsConstructor
@EnableConfigurationProperties({ RedisDistributedProperties.class, BloomFilterPenetrateProperties.class })
public class CacheAutoConfiguration {
    /**
     * 分布式缓存配置
     */
    private final RedisDistributedProperties redisDistributedProperties;

    /**
     * 创建 Redis Key 序列化器，可自定义 Key Prefix
     */
    @Bean
    public RedisKeySerializer redisKeySerializer() {
        // 从配置属性中获取缓存键的前缀和字符集
        String prefix = redisDistributedProperties.getPrefix();
        // 从配置属性中获取缓存键的前缀字符集
        String prefixCharset = redisDistributedProperties.getPrefixCharset();
        // 创建 Redis Key 序列化器，设置缓存键的前缀和字符集
        return new RedisKeySerializer(prefix, prefixCharset);
    }

    /**
     * 防止缓存穿透的布隆过滤器
     */
    @Bean
    // 从配置属性中获取布隆过滤器的默认实例名称
    @ConditionalOnProperty(prefix = BloomFilterPenetrateProperties.PREFIX, name = "enabled", havingValue = "true")
    public RBloomFilter<String> cachePenetrationBloomFilter(RedissonClient redissonClient,
                                                            BloomFilterPenetrateProperties bloomFilterPenetrateProperties) {
        // 从 Redisson 客户端中获取布隆过滤器实例
        RBloomFilter<String> cachePenetrationBloomFilter = redissonClient
                .getBloomFilter(bloomFilterPenetrateProperties.getName());
        // 初始化布隆过滤器，设置预期插入量和预期错误概率
        cachePenetrationBloomFilter.tryInit(bloomFilterPenetrateProperties.getExpectedInsertions(),
                bloomFilterPenetrateProperties.getFalseProbability());
        // 返回初始化后的布隆过滤器实例
        return cachePenetrationBloomFilter;
    }

    @Bean
    // 静态代理模式: Redis 客户端代理类增强
    public StringRedisTemplateProxy stringRedisTemplateProxy(RedisKeySerializer redisKeySerializer,
                                                             StringRedisTemplate stringRedisTemplate,
                                                             RedissonClient redissonClient) {
        // 设置 Redis 模板的 Key 序列化器为自定义的 Redis Key 序列化器
        stringRedisTemplate.setKeySerializer(redisKeySerializer);
        // 创建并返回 Redis 模板代理类实例
        return new StringRedisTemplateProxy(stringRedisTemplate, redisDistributedProperties, redissonClient);
    }
}
