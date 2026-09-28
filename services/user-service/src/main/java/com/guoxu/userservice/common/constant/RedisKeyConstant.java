package com.guoxu.userservice.common.constant;

/**
 * RedisKeyConstant
 * Redis Key 定义常量类
 * @author 执笔画棠
 * @date 2025/11/18 16:00
 **/
public final class RedisKeyConstant {
    /**
     * 用户注册锁，Key Prefix + 用户名
     */
    public static final String LOCK_USER_REGISTER = "index12306-user-service:lock:user-register:";

    /**
     * 用户注销锁，Key Prefix + 用户名
     */
    public static final String USER_DELETION = "index12306-user-service:user-deletion:";

    /**
     * 用户注册可复用用户名分片，Key Prefix + Idx
     */
    public static final String USER_REGISTER_REUSE_SHARDING = "index12306-user-service:user-reuse:";

    /**
     * 用户乘车人列表，Key Prefix + 用户名
     */
    public static final String USER_PASSENGER_LIST = "index12306-user-service:user-passenger-list:";
}
