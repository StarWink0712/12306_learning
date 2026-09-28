package com.guoxu.toolkit;

import com.guoxu.core.snowflake.Snowflake;
import com.guoxu.core.snowflake.SnowflakeIdInfo;
import com.guoxu.handler.IdGeneratorManager;

/**
 * SnowflakeIdUtil
 *
 * @author 执笔画棠
 * @date 2025/11/06 17:15
 **/
public class SnowflakeIdUtil {
    /**
     * 雪花算法对象
     */
    private static Snowflake SNOWFLAKE;

    /**
     * 初始化雪花算法
     */
    public static void initSnowflake(Snowflake snowflake) {
        SnowflakeIdUtil.SNOWFLAKE = snowflake;
    }

    /**
     * 获取雪花算法实例
     */
    public static Snowflake getInstance() {
        return SNOWFLAKE;
    }

    /**
     * 获取雪花算法下一个 ID
     */
    public static long nextId() {
        return SNOWFLAKE.nextId();
    }

    /**
     * 获取雪花算法下一个字符串类型 ID
     */
    public static String nextIdStr() {
        return Long.toString(nextId());
    }

    /**
     * 解析雪花算法生成的 ID 为对象
     */
    public static SnowflakeIdInfo parseSnowflakeId(String snowflakeId) {
        return SNOWFLAKE.parseSnowflakeId(Long.parseLong(snowflakeId));
    }

    /**
     * 解析雪花算法生成的 ID 为对象
     */
    public static SnowflakeIdInfo parseSnowflakeId(long snowflakeId) {
        return SNOWFLAKE.parseSnowflakeId(snowflakeId);
    }

    /**
     * 根据 {@param serviceId} 生成雪花算法 ID
     */
    public static long nextIdByService(String serviceId) {
        return IdGeneratorManager.getDefaultServiceIdGenerator().nextId(Long.parseLong(serviceId));
    }

    /**
     * 根据 {@param serviceId} 生成字符串类型雪花算法 ID
     */
    public static String nextIdStrByService(String serviceId) {
        return IdGeneratorManager.getDefaultServiceIdGenerator().nextIdStr(Long.parseLong(serviceId));
    }

    /**
     * 根据 {@param serviceId} 生成字符串类型雪花算法 ID
     */
    public static String nextIdStrByService(String resource, long serviceId) {
        return IdGeneratorManager.getIdGenerator(resource).nextIdStr(serviceId);
    }

    /**
     * 根据 {@param serviceId} 生成字符串类型雪花算法 ID
     */
    public static String nextIdStrByService(String resource, String serviceId) {
        return IdGeneratorManager.getIdGenerator(resource).nextIdStr(serviceId);
    }


    public static SnowflakeIdInfo parseSnowflakeServiceId(String snowflakeId) {
        return IdGeneratorManager.getDefaultServiceIdGenerator().parseSnowflakeId(Long.parseLong(snowflakeId));
    }


    public static SnowflakeIdInfo parseSnowflakeServiceId(String resource, String snowflakeId) {
        return IdGeneratorManager.getIdGenerator(resource).parseSnowflakeId(Long.parseLong(snowflakeId));
    }
}
