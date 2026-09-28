package com.guoxu.handler;

import com.guoxu.core.IdGenerator;
import com.guoxu.core.serviceid.DefaultServiceIdGenerator;
import com.guoxu.core.serviceid.ServiceIdGenerator;
import lombok.NonNull;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * IdGeneratorManager
 * id生成器管理器，管理各个id生成器
 * 这里总共有两个id生成器，分别是雪花算法id生成器和服务id生成器
 * @author 执笔画棠
 * @date 2025/11/06 17:15
 **/
public class IdGeneratorManager {
    /**
     * ID 生成器管理容器
     */
    private static Map<String, IdGenerator> MANAGER = new ConcurrentHashMap<>();

    /**
     * 注册默认 ID 生成器
     */
    static {
        MANAGER.put("default", new DefaultServiceIdGenerator());
    }

    /**
     * 注册 ID 生成器
     */
    public static void registerIdGenerator(@NonNull String resource, @NonNull IdGenerator idGenerator) {
        IdGenerator actual = MANAGER.get(resource);
        if (actual != null) {
            return;
        }
        MANAGER.put(resource, idGenerator);
    }

    /**
     * 根据 {@param resource} 获取 ID 生成器
     */
    public static ServiceIdGenerator getIdGenerator(@NonNull String resource) {
        return Optional.ofNullable(MANAGER.get(resource)).map(each -> (ServiceIdGenerator) each).orElse(null);
    }

    /**
     * 获取默认 ID 生成器 {@link DefaultServiceIdGenerator}
     */
    public static ServiceIdGenerator getDefaultServiceIdGenerator() {
        return Optional.ofNullable(MANAGER.get("default")).map(each -> (ServiceIdGenerator) each).orElse(null);
    }
}
