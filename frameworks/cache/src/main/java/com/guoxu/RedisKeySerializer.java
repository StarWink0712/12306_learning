package com.guoxu;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * RedisKeySerializer rediskey序列化
 *用于将 Redis Key 进行序列化和反序列化操作。
 *通常情况下，Redis Key 是字符串类型，而在存储到 Redis 中时，需要将其转换为字节数组。
 *该序列化器会在 Key 前添加一个前缀，用于区分不同的缓存 Key。
 * 这是一个设计良好、目标明确的工具类，完美地实践了面向切面编程 (AOP) 和依赖注入 (DI) 的思想，
 * 将 Key 的通用处理逻辑（加前缀、统一编码）从业务代码中抽离出来，实现了关注点分离。
 * @author 执笔画棠
 * @date 2025/11/05 19:16
 **/
@RequiredArgsConstructor
public class RedisKeySerializer implements InitializingBean, RedisSerializer<String> {

    //缓存健的前缀
    private final String keyPrefix;

    //缓存键的字符集名称
    private final String charsetName;

    //缓存键的字符集
    private Charset charset;


    //bean属性设置完成后调用，用于初始化字符集
    @Override
    public void afterPropertiesSet() throws Exception {
        charset= Charset.forName(charsetName);

    }
    //序列化key,将key转换为字节数组，然后存到redis中
    @Override
    public byte[] serialize(String value) throws SerializationException {
        String builderKey=keyPrefix+value;
        return builderKey.getBytes();
    }

    //反序列化
    @Override
    public String deserialize(byte[] bytes) throws SerializationException {
        return new String(bytes,charset);
    }
}
