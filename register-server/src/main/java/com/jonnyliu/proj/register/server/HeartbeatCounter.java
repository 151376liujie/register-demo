package com.jonnyliu.proj.register.server;

import java.util.concurrent.atomic.LongAdder;

/**
 * 心跳测量计数器
 *
 * @author liujie
 */
public class HeartbeatCounter {

    public static final int ONE_MINUTE = 60 * 1000;

    private static final HeartbeatCounter INSTANCE = new HeartbeatCounter();

    /**
     * 最近一分钟的时间戳
     */
    private long lastMinuteTimestamp;

    /**
     * 最近一分钟的心跳次数
     */
    private LongAdder lastMinuteHeartbeatRate;

    private HeartbeatCounter() {
        this.lastMinuteHeartbeatRate = new LongAdder();
        this.lastMinuteTimestamp = System.currentTimeMillis();
    }

    public static HeartbeatCounter getInstance() {
        return INSTANCE;
    }

    /**
     * 增加最近一分钟的心跳次数
     */
    public synchronized void increment() {
        long currentTimeMillis = System.currentTimeMillis();
        if (currentTimeMillis - lastMinuteTimestamp < ONE_MINUTE) {
            this.lastMinuteHeartbeatRate.increment();
        } else {
            this.lastMinuteHeartbeatRate = new LongAdder();
            this.lastMinuteTimestamp = System.currentTimeMillis();
        }
    }

    /**
     * 获取最近一分钟的心跳次数
     *
     * @return 最近一分钟的心跳次数
     */
    public synchronized long getLastMinuteHeartbeatRate() {
        return this.lastMinuteHeartbeatRate.longValue();
    }
}