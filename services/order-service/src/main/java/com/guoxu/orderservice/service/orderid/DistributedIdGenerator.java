package com.guoxu.orderservice.service.orderid;

/**
 * DistributedIdGenerator 全局唯一订单号生成器
 *
 * @author 执笔画棠
 * @date 2025/11/09 21:26
 **/
public class DistributedIdGenerator {

    // 起始时间戳（2021-01-01 00:00:00 UTC），用于减少生成ID的长度
    // 从这个时间点开始计算时间差，可以支持更长时间的使用
    private static final long EPOCH = 1609459200000L;

    // 节点ID所占的比特位数，5位可以支持32个节点
    private static final int NODE_BITS = 5;

    // 序列号所占的比特位数，7位可以支持每毫秒128个序列号
    private static final int SEQUENCE_BITS = 7;

    // 当前节点的ID，不同的分布式节点需要配置不同的ID
    private final long nodeID;

    // 上次生成ID的时间戳，用于检测时钟回拨和计算时间差
    private long lastTimestamp = -1L;

    // 当前毫秒内的序列号，用于区分同一毫秒内生成的多个ID
    private long sequence = 0L;

    /**
     * 构造函数，初始化分布式ID生成器
     *
     * @param nodeID 节点ID，不同的分布式节点必须使用不同的ID
     *               范围：0 到 (2^NODE_BITS - 1)，即0-31
     */
    public DistributedIdGenerator(long nodeID) {
        // 将传入的节点ID赋值给实例变量
        this.nodeID = nodeID;
    }

    /**
     * 生成分布式唯一ID（线程安全）
     * ID结构：时间戳 | 节点ID | 序列号
     * 加锁确保多线程下只有一个线程能进入这个方法
     * 主要是为了保护两个共享可变状态
     * lasttimestamp上一次生成id的时间戳
     * sequence当前毫秒内的序列号
     * @return 生成的64位长整型唯一ID
     * @throws RuntimeException 当时钟回拨时抛出异常
     */
    public synchronized long generateId() {
        // 计算当前时间与起始时间的时间差（毫秒）
        long timestamp = System.currentTimeMillis() - EPOCH;

        // 检查时钟回拨：如果当前时间小于上次生成ID的时间，说明发生了时钟回拨
        if (timestamp < lastTimestamp) {
            // 抛出运行时异常，拒绝在时钟回拨的情况下生成ID，避免ID冲突
            throw new RuntimeException("Clock moved backwards. Refusing to generate ID.");
        }

        // 如果当前时间戳与上次生成ID的时间戳相同（同一毫秒内）
        if (timestamp == lastTimestamp) {
            // 序列号加1，并与序列号掩码进行按位与操作，防止序列号溢出
            // (1 << SEQUENCE_BITS) - 1 创建序列号的掩码，例如7位序列号掩码为127
            sequence = (sequence + 1) & ((1 << SEQUENCE_BITS) - 1);

            // 如果序列号归零，说明当前毫秒内的序列号已用完
            if (sequence == 0) {
                // 等待到下一毫秒再生成ID
                timestamp = tilNextMillis(lastTimestamp);
            }
        } else {
            // 如果是新的毫秒，重置序列号为0
            sequence = 0L;
        }

        // 更新最后时间戳为当前时间戳
        lastTimestamp = timestamp;

        // 组合生成最终ID：时间戳左移(节点位数+序列号位数) | 节点ID左移序列号位数 | 序列号
        // 这样ID的结构就是：高位是时间戳，中间是节点ID，低位是序列号
        return (timestamp << (NODE_BITS + SEQUENCE_BITS)) | (nodeID << SEQUENCE_BITS) | sequence;
    }

    /**
     * 等待直到下一毫秒
     * 当当前毫秒的序列号用完时，调用此方法等待时间推进到下一毫秒
     *
     * @param lastTimestamp 上次生成ID的时间戳
     * @return 下一毫秒的时间戳
     */
    private long tilNextMillis(long lastTimestamp) {
        // 获取当前时间戳
        long timestamp = System.currentTimeMillis() - EPOCH;

        // 循环等待，直到时间戳大于上次时间戳（即等到下一毫秒）
        while (timestamp <= lastTimestamp) {
            // 重新获取当前时间戳，继续等待
            timestamp = System.currentTimeMillis() - EPOCH;
        }

        // 返回下一毫秒的时间戳
        return timestamp;
    }
}
