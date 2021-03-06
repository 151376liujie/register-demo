package com.jonnyliu.proj.register.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 自我保护机制
 *
 * @author liujie
 */
public class SelfProtectionPolicy {

    /**
     * 自我保护机制的阈值，当 实际的心跳次数  < 每分钟期望的心跳次数 * 0.85，则注册中心会自动进入自我保护机制
     */
    public static final double THRESHOLD_SELF_PROTECTION = 0.85F;
    private static final Logger log = LoggerFactory.getLogger(SelfProtectionPolicy.class);
    private static final SelfProtectionPolicy INSTANCE = new SelfProtectionPolicy();

    private SelfProtectionPolicy() {
    }

    /**
     * 期望的一个心跳次数,如果你有10个服务实例,此时这个值就是10 * 2 = 20
     */
    private long expectedHeartbeatRate;

    /**
     * 期望的心跳次数阈值, 10 * 2 * 0.85 = 17, 每分钟至少有17次心跳,才不会进入自我保护机制
     */
    private long expectedHeartbeatThreshold = 0L;

    public static SelfProtectionPolicy getInstance() {
        return INSTANCE;
    }

    public long getExpectedHeartbeatRate() {
        return expectedHeartbeatRate;
    }

    public void setExpectedHeartbeatRate(long expectedHeartbeatRate) {
        this.expectedHeartbeatRate = expectedHeartbeatRate;
        this.expectedHeartbeatThreshold = (long) (this.expectedHeartbeatRate * THRESHOLD_SELF_PROTECTION);
    }

    public long getExpectedHeartbeatThreshold() {
        return this.expectedHeartbeatThreshold;
    }

    /**
     * 判断自我保护机制是否激活
     *
     * @return
     */
    public boolean isSelfProtectionActivated() {
        HeartbeatCounter heartbeatCounter = HeartbeatCounter.getInstance();
        log.info("最近一分钟的心跳次数为: {}, 期望的最小心跳次数为: {}", heartbeatCounter.getLastMinuteHeartbeatRate(),
                expectedHeartbeatThreshold);
        return heartbeatCounter.getLastMinuteHeartbeatRate() < expectedHeartbeatThreshold;
    }

    @Override
    public String toString() {
        return "SelfProtectionPolicy{" +
                "expectedHeartbeatRate=" + expectedHeartbeatRate +
                ", expectedHeartbeatThreshold=" + expectedHeartbeatThreshold +
                '}';
    }
}