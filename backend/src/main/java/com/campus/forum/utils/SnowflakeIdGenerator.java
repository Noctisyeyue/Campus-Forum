package com.campus.forum.utils;

import org.springframework.stereotype.Component;

/**
 * 雪花算法 ID 生成器，生成全局唯一趋势递增的 64 位 ID
 */
@Component
public class SnowflakeIdGenerator {

    // 起始时间戳（2023-08-04）
    private static final long START_TIMESTAMP = 1691087910202L;

    private static final long DATA_CENTER_ID_BITS = 5L;  // 数据中心 ID 占用位数
    private static final long WORKER_ID_BITS = 5L;       // 机器 ID 占用位数
    private static final long SEQUENCE_BITS = 12L;       // 序列号占用位数

    private static final long MAX_DATA_CENTER_ID = ~(-1L << DATA_CENTER_ID_BITS);  // 数据中心 ID 最大值：31
    private static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);            // 机器 ID 最大值：31
    private static final long MAX_SEQUENCE = ~(-1L << SEQUENCE_BITS);              // 序列号最大值：4095

    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;
    private static final long DATA_CENTER_ID_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;
    private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS + DATA_CENTER_ID_BITS;

    private final long dataCenterId;    // 数据中心 ID
    private final long workerId;        // 机器 ID
    private long lastTimestamp = -1L;    // 上次生成 ID 的时间戳
    private long sequence = 0L;         // 同一毫秒内的序列号

    /**
     * 默认构造，数据中心ID和机器ID均为1
     */
    public SnowflakeIdGenerator() {
        this(1, 1);
    }

    /**
     * 指定数据中心ID和机器ID构造
     *
     * @param dataCenterId 数据中心ID（0~31）
     * @param workerId     机器ID（0~31）
     */
    private SnowflakeIdGenerator(long dataCenterId, long workerId) {
        if (dataCenterId > MAX_DATA_CENTER_ID || dataCenterId < 0) {
            throw new IllegalArgumentException("Data center ID can't be greater than " + MAX_DATA_CENTER_ID + " or less than 0");
        }
        if (workerId > MAX_WORKER_ID || workerId < 0) {
            throw new IllegalArgumentException("Worker ID can't be greater than " + MAX_WORKER_ID + " or less than 0");
        }
        this.dataCenterId = dataCenterId;
        this.workerId = workerId;
    }

    /**
     * 生成下一个唯一 ID（线程安全）
     *
     * @return 全局唯一的 64 位 ID
     */
    public synchronized long nextId() {
        long timestamp = getCurrentTimestamp();
        if (timestamp < lastTimestamp) {
            throw new IllegalStateException("Clock moved backwards. Refusing to generate ID.");
        }
        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) & MAX_SEQUENCE;
            if (sequence == 0) {
                timestamp = getNextTimestamp(lastTimestamp);
            }
        } else {
            sequence = 0L;
        }
        lastTimestamp = timestamp;
        return ((timestamp - START_TIMESTAMP) << TIMESTAMP_SHIFT) |
                (dataCenterId << DATA_CENTER_ID_SHIFT) |
                (workerId << WORKER_ID_SHIFT) |
                sequence;
    }

    /**
     * 获取当前时间戳
     *
     * @return 当前毫秒时间戳
     */
    private long getCurrentTimestamp() {
        return System.currentTimeMillis();
    }

    /**
     * 自旋等待直到超过上次时间戳
     *
     * @param lastTimestamp 上次生成ID的时间戳
     * @return 新的时间戳
     */
    private long getNextTimestamp(long lastTimestamp) {
        long timestamp = getCurrentTimestamp();
        while (timestamp <= lastTimestamp) {
            timestamp = getCurrentTimestamp();
        }
        return timestamp;
    }
}
