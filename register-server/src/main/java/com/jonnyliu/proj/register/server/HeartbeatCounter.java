package com.jonnyliu.proj.register.server;

import java.util.concurrent.atomic.LongAdder;

/**
 * 心跳测量计数器
 *
 * @author liujie
 */
public class HeartbeatCounter {

    public static final int ONE_MINUTE = 60 * 1000;

    private static HeartbeatCounter instance = new HeartbeatCounter();

    /**
     * 最近一分钟的时间戳
     */
    private long lastMinuteTimestamp = System.currentTimeMillis();

    /**
     * 最近一分钟的心跳次数
     */
    private LongAdder lastMinuteHeartbeatRate = new LongAdder();

    public static HeartbeatCounter getInstance() {
        return instance;
    }

    private HeartbeatCounter() {
    }

    /**
     * 增加最近一分钟的心跳次数
     */
    public void increment() {
        long currentTimeMillis = System.currentTimeMillis();
        if (currentTimeMillis - lastMinuteTimestamp < ONE_MINUTE) {
            lastMinuteHeartbeatRate.increment();
        } else {
            lastMinuteHeartbeatRate = new LongAdder();
            lastMinuteTimestamp = System.currentTimeMillis();
        }
    }

    /**
     * 获取最近一分钟的心跳次数
     *
     * @return 最近一分钟的心跳次数
     */
    public long getLastMinuteHeartbeatRate() {
        return lastMinuteHeartbeatRate.longValue();
    }
}
