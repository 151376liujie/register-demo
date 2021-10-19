package com.jonnyliu.proj.register.server;

import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 心跳测量计数器
 *
 * @author liujie
 */
public class HeartbeatCounter {

    private static final Logger log = LoggerFactory.getLogger(HeartbeatCounter.class);
    private static final int ONE_MINUTE = 60 * 1000;

    private static final HeartbeatCounter INSTANCE = new HeartbeatCounter();

    /**
     * 最近一分钟的时间戳
     */
    private long lastMinuteTimestamp;

    /**
     * 最近一分钟的心跳次数
     */
    private AtomicLong lastMinuteHeartbeatRate;

    private HeartbeatCounter() {
        this.lastMinuteHeartbeatRate = new AtomicLong();
        this.lastMinuteTimestamp = System.currentTimeMillis();
        //开启一个后台线程
        Daemon daemon = new Daemon();
        daemon.setDaemon(true);
        daemon.setName("HEARTBEAT-COUNTER-MONITOR-THREAD");
        daemon.start();
    }

    public static HeartbeatCounter getInstance() {
        return INSTANCE;
    }

    /**
     * 增加最近一分钟的心跳次数
     */
    public void increment() {
        long currentTimeMillis = System.currentTimeMillis();
        if (currentTimeMillis - lastMinuteTimestamp < ONE_MINUTE) {
            this.lastMinuteHeartbeatRate.incrementAndGet();
        }
    }

    /**
     * 获取最近一分钟的心跳次数
     *
     * @return 最近一分钟的心跳次数
     */
    public long getLastMinuteHeartbeatRate() {
        return this.lastMinuteHeartbeatRate.get();
    }

    private class Daemon extends Thread {

        @Override
        public void run() {
            while (true) {
                long currentTimeMillis = System.currentTimeMillis();
                if (currentTimeMillis - lastMinuteTimestamp > ONE_MINUTE) {
                    log.info("超过一分钟，清空心跳次数");
                    while (true) {
                        long expected = lastMinuteHeartbeatRate.get();
                        if (lastMinuteHeartbeatRate.compareAndSet(expected, 0L)) {
                            break;
                        }
                    }
                    lastMinuteTimestamp = System.currentTimeMillis();
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    log.error("error.", e);
                }
            }
        }
    }
}