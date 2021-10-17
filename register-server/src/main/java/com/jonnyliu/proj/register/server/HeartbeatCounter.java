package com.jonnyliu.proj.register.server;

import java.util.concurrent.atomic.LongAdder;
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
    private LongAdder lastMinuteHeartbeatRate;

    private HeartbeatCounter() {
        this.lastMinuteHeartbeatRate = new LongAdder();
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
        synchronized (HeartbeatCounter.class) {
            long currentTimeMillis = System.currentTimeMillis();
            if (currentTimeMillis - lastMinuteTimestamp < ONE_MINUTE) {
                this.lastMinuteHeartbeatRate.increment();
            }
        }
    }

    /**
     * 获取最近一分钟的心跳次数
     *
     * @return 最近一分钟的心跳次数
     */
    public long getLastMinuteHeartbeatRate() {
        synchronized (HeartbeatCounter.class) {
            return this.lastMinuteHeartbeatRate.longValue();
        }
    }

    private class Daemon extends Thread {

        @Override
        public void run() {
            while (true) {
                synchronized (HeartbeatCounter.class) {
                    long currentTimeMillis = System.currentTimeMillis();
                    if (currentTimeMillis - lastMinuteTimestamp > ONE_MINUTE) {
                        log.info("超过一分钟，清空心跳次数");
                        lastMinuteHeartbeatRate = new LongAdder();
                        lastMinuteTimestamp = System.currentTimeMillis();
                    }
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