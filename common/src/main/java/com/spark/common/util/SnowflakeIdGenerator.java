package com.spark.common.util;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 雪花算法ID生成器
 * 生成64位唯一ID，包含时间戳、机器ID、序列号
 */
public class SnowflakeIdGenerator {
    /**
     * 起始时间戳 (2024-01-01 00:00:00)
     */
    private static final long START_TIMESTAMP = 1704067200000L;

    /**
     * 机器ID占用的位数
     */
    private static final long MACHINE_ID_BITS = 5L;

    /**
     * 数据中心ID占用的位数
     */
    private static final long DATACENTER_ID_BITS = 5L;

    /**
     * 序列号占用的位数
     */
    private static final long SEQUENCE_BITS = 12L;

    /**
     * 机器ID最大值
     */
    private static final long MAX_MACHINE_ID = ~(-1L << MACHINE_ID_BITS);

    /**
     * 数据中心ID最大值
     */
    private static final long MAX_DATACENTER_ID = ~(-1L << DATACENTER_ID_BITS);

    /**
     * 序列号最大值
     */
    private static final long MAX_SEQUENCE = ~(-1L << SEQUENCE_BITS);

    /**
     * 机器ID向左移12位
     */
    private static final long MACHINE_ID_SHIFT = SEQUENCE_BITS;

    /**
     * 数据中心ID向左移17位(12+5)
     */
    private static final long DATACENTER_ID_SHIFT = SEQUENCE_BITS + MACHINE_ID_BITS;

    /**
     * 时间戳向左移22位(5+5+12)
     */
    private static final long TIMESTAMP_LEFT_SHIFT = SEQUENCE_BITS + MACHINE_ID_BITS + DATACENTER_ID_BITS;

    /**
     * 机器ID
     */
    private final long machineId;

    /**
     * 数据中心ID
     */
    private final long datacenterId;

    /**
     * 序列号
     */
    private final AtomicLong sequence = new AtomicLong(0L);

    /**
     * 上次生成ID的时间戳
     */
    private volatile long lastTimestamp = -1L;

    /**
     * 构造函数
     *
     * @param machineId    机器ID (0-31)
     * @param datacenterId 数据中心ID (0-31)
     */
    public SnowflakeIdGenerator(long machineId, long datacenterId) {
        if (machineId > MAX_MACHINE_ID || machineId < 0) {
            throw new IllegalArgumentException("machineId can't be greater than " + MAX_MACHINE_ID + " or less than 0");
        }
        if (datacenterId > MAX_DATACENTER_ID || datacenterId < 0) {
            throw new IllegalArgumentException("datacenterId can't be greater than " + MAX_DATACENTER_ID + " or less than 0");
        }
        this.machineId = machineId;
        this.datacenterId = datacenterId;
    }

    /**
     * 生成下一个ID
     *
     * @return 唯一ID
     */
    public synchronized long nextId() {
        long timestamp = System.currentTimeMillis();

        // 如果当前时间小于上一次ID生成的时间戳，说明系统时钟回退过，抛出异常
        if (timestamp < lastTimestamp) {
            throw new RuntimeException("Clock moved backwards. Refusing to generate id");
        }

        // 如果是同一时间生成的，则进行毫秒内序列
        if (timestamp == lastTimestamp) {
            long seq = sequence.incrementAndGet();
            // 毫秒内序列溢出
            if (seq > MAX_SEQUENCE) {
                // 阻塞到下一个毫秒，获得新的时间戳
                timestamp = tilNextMillis(lastTimestamp);
                sequence.set(0L);
            }
        } else {
            // 时间戳改变，毫秒内序列重置
            sequence.set(0L);
        }

        // 上次生成ID的时间戳
        lastTimestamp = timestamp;

        // 移位并通过或运算拼到一起组成64位的ID
        return ((timestamp - START_TIMESTAMP) << TIMESTAMP_LEFT_SHIFT)
                | (datacenterId << DATACENTER_ID_SHIFT)
                | (machineId << MACHINE_ID_SHIFT)
                | sequence.get();
    }

    /**
     * 阻塞到下一个毫秒，直到获得新的时间戳
     *
     * @param lastTimestamp 上次生成ID的时间戳
     * @return 当前时间戳
     */
    private long tilNextMillis(long lastTimestamp) {
        long timestamp = System.currentTimeMillis();
        while (timestamp <= lastTimestamp) {
            timestamp = System.currentTimeMillis();
        }
        return timestamp;
    }

    /**
     * 默认实例（机器ID=0，数据中心ID=0）
     */
    private static final SnowflakeIdGenerator DEFAULT_INSTANCE = new SnowflakeIdGenerator(0, 0);

    /**
     * 获取默认实例
     *
     * @return 默认ID生成器实例
     */
    public static SnowflakeIdGenerator getDefault() {
        return DEFAULT_INSTANCE;
    }
}
