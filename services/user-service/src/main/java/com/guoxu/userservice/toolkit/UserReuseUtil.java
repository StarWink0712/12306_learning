package com.guoxu.userservice.toolkit;

import static com.guoxu.userservice.common.constant.Index12306Constant.USER_REGISTER_REUSE_SHARDING_COUNT;

/**
 * UserReuseUtil
 * 用户名可复用工具类
 * @author 执笔画棠
 * @date 2025/11/18 16:44
 **/
public class UserReuseUtil {
    /**
     * 计算分片位置
     */
    public static int hashShardingIdx(String username) {
        return Math.abs(username.hashCode() % USER_REGISTER_REUSE_SHARDING_COUNT);
    }
}
