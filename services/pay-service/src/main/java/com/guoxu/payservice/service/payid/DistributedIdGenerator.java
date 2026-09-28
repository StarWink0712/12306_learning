package com.guoxu.payservice.service.payid;

/**
 * DistributedIdGenerator
 * 全局唯一订单号生成器
 * @author 执笔画棠
 * @date 2025/11/11 18:33
 **/
public class DistributedIdGenerator {
    // 起始时间戳（毫秒级），对应2021-01-01 00:00:00，用于计算相对时间（减少ID长度）
    private static final long EPOCH = 1609459200000L;
    // 节点ID占用的位数（5位），最多支持2^5=32个不同节点
    private static final int NODE_BITS = 5;
    // 序列号占用的位数（7位），同一毫秒内最多生成2^7=128个不同ID
    private static final int SEQUENCE_BITS = 7;

    // 当前生成器所属的节点ID（需保证分布式环境中唯一）
    private final long nodeID;
    // 记录上一次生成ID的时间戳（基于EPOCH的相对时间）
    private long lastTimestamp = -1L;
    // 序列号，用于同一毫秒内生成多个ID时区分（从0开始递增）
    private long sequence = 0L;

    // 构造方法：初始化节点ID
    // 参数nodeID：当前节点的唯一标识，需确保不超过NODE_BITS所能表示的最大值（即31）
    public DistributedIdGenerator(long nodeID) {
        this.nodeID = nodeID;
    }

    // 生成分布式ID的核心方法（加锁保证线程安全）
    public synchronized long generateId() {
        // 获取当前时间戳相对于起始时间EPOCH的差值（减少ID的绝对时间部分长度）
        long timestamp = System.currentTimeMillis() - EPOCH;

        // 若当前时间戳小于上一次生成ID的时间戳，说明系统时钟回拨（异常情况）
        if (timestamp < lastTimestamp) {
            throw new RuntimeException("Clock moved backwards. Refusing to generate ID.");
        }

        // 若当前时间戳与上一次相同（同一毫秒内）
        if (timestamp == lastTimestamp) {
            // 序列号自增1，并用掩码限制在SEQUENCE_BITS范围内（防止溢出）
            // 掩码计算：(1 << SEQUENCE_BITS) - 1 等价于 0b1111111（7位全为1）
            sequence = (sequence + 1) & ((1 << SEQUENCE_BITS) - 1);
            // 若序列号自增后归0，说明当前毫秒的序列号已用尽，需等待到下一毫秒
            if (sequence == 0) {
                timestamp = tilNextMillis(lastTimestamp);
            }
        } else {
            // 若当前时间戳大于上一次，说明进入新的毫秒，重置序列号为0
            sequence = 0L;
        }

        // 更新上一次生成ID的时间戳为当前时间戳
        lastTimestamp = timestamp;

        // 组合生成最终ID：
        // 1. 时间戳左移（节点位数+序列号位数），占据最高位
        // 2. 节点ID左移序列号位数，占据中间位
        // 3. 序列号占据最低位
        // 三者通过按位或（|）拼接，保证ID唯一且有序（按时间递增）
        return (timestamp << (NODE_BITS + SEQUENCE_BITS)) | (nodeID << SEQUENCE_BITS) | sequence;
    }

    // 等待到下一个毫秒的工具方法（确保时间戳递增）
    // 参数lastTimestamp：上一次生成ID的时间戳
    // 返回值：大于lastTimestamp的新时间戳（基于EPOCH的相对时间）
    private long tilNextMillis(long lastTimestamp) {
        // 获取当前时间相对于EPOCH的时间戳
        long timestamp = System.currentTimeMillis() - EPOCH;
        // 循环等待，直到当前时间戳超过上一次的时间戳（避免时钟回拨或未到下一毫秒）
        while (timestamp <= lastTimestamp) {
            timestamp = System.currentTimeMillis() - EPOCH;
        }
        return timestamp;
    }
}
