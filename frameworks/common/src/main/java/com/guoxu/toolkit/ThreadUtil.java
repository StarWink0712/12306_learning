package com.guoxu.toolkit;

import lombok.SneakyThrows;

/**
 * ThreadUtil
 *
 * @author 执笔画棠
 * @date 2025/11/04 18:20
 **/
public class ThreadUtil {
    /**
     * 睡眠当前线程指定时间 {@param millis}
     *
     * @param millis 睡眠时间，单位毫秒
     */
    @SneakyThrows(value = InterruptedException.class)
    public static void sleep(long millis) {
        Thread.sleep(millis);
    }
}
