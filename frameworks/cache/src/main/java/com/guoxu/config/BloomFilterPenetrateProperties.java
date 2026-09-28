package com.guoxu.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * BloomFilterPenetrateProperties 布隆过滤器配置
 *
 * @author 执笔画棠
 * @date 2025/11/05 19:43
 **/
@Data
@ConfigurationProperties(prefix = BloomFilterPenetrateProperties.PREFIX)
public class BloomFilterPenetrateProperties {

    /**
     * 缓存穿透布隆过滤器默认配置前缀
     */
    public static final String PREFIX = "framework.cache.redis.bloom-filter.default";

    /**
     * 布隆过滤器默认实例名称
     */
    private String name = "cache_penetration_bloom_filter";

    /**
     * 每个元素的预期插入量
     */
    private Long expectedInsertions = 64L;

    /**
     * 预期错误概率
     */
    private Double falseProbability = 0.03D;
}
