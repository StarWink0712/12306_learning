package com.guoxu.core.snowflake;

import cn.hutool.core.collection.CollUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import com.guoxu.ApplicationContextHolder;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;

import java.util.ArrayList;
import java.util.List;

/**
 * LocalRedisWorkIdChoose
 * 使用 Redis 获取雪花 WorkId
 * @author 执笔画棠
 * @date 2025/11/06 17:28
 **/
@Slf4j
public class LocalRedisWorkIdChoose extends AbstractWorkIdChooseTemplate implements InitializingBean {

    // Redis 客户端
    private RedisTemplate stringRedisTemplate;

    public LocalRedisWorkIdChoose() {
        this.stringRedisTemplate = ApplicationContextHolder.getBean(StringRedisTemplate.class);
    }

    /**
     * 选择 WorkId 生成器
     *
     * @return WorkId 包装器
     */
    @Override
    public WorkIdWrapper chooseWorkId() {
        // 执行 Lua 脚本获取 WorkId
        // defaultredisscript 是 Spring Data Redis 提供的一个类，用于执行 Redis Lua 脚本。
        DefaultRedisScript redisScript = new DefaultRedisScript();
        // 设置 Lua 脚本来源
        redisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("lua/chooseWorkIdLua.lua")));
        List<Long> luaResultList = null;
        try {
            // 设置 Lua 脚本返回值类型
            redisScript.setResultType(List.class);
            // 执行 Lua 脚本
            luaResultList = (ArrayList) this.stringRedisTemplate.execute(redisScript, null);
        } catch (Exception ex) {
            log.error("Redis Lua 脚本获取 WorkId 失败", ex);
        }
        // 如果 Lua 脚本执行结果为空，使用随机 WorkId 生成器
        return CollUtil.isNotEmpty(luaResultList) ? new WorkIdWrapper(luaResultList.get(0), luaResultList.get(1))
                : new RandomWorkIdChoose().chooseWorkId();
    }

    /**
     * 初始化 Snowflake 算法
     * 它实现了 Spring 的 InitializingBean 接口。
     * 这是一个关键的设计，意味着当 Spring 容器创建并初始化好这个 Bean
     * 之后，会自动调用 afterPropertiesSet() 方法。
     *
     * @throws Exception
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        // 初始化 Snowflake 算法
        // AbstractWorkIdChooseTemplate即他的chooseandinit方法，
        // 将创建好的 Snowflake 实例注册到全局的 SnowflakeIdUtil 工具类中,这是整个流程的闭环
        // 从此以后，应用程序的任何地方都可以通过 SnowflakeIdUtil.nextId() 来使用这个已经初始化好的 ID 生成器了。
        chooseAndInit();
    }
}
