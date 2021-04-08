package com.jonnyliu.proj.register.server;

/**
 * 自我保护机制
 *
 * @author liujie
 */
public class SelfProtectionPolicy {

    private static final SelfProtectionPolicy instance = new SelfProtectionPolicy();

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
        return instance;
    }

    public long getExpectedHeartbeatRate() {
        return expectedHeartbeatRate;
    }

    public void setExpectedHeartbeatRate(long expectedHeartbeatRate) {
        this.expectedHeartbeatRate = expectedHeartbeatRate;
    }

    public long getExpectedHeartbeatThreshold() {
        return expectedHeartbeatThreshold;
    }

    public void setExpectedHeartbeatThreshold(long expectedHeartbeatThreshold) {
        this.expectedHeartbeatThreshold = expectedHeartbeatThreshold;
    }

    /**
     * 判断自我保护机制是否激活
     *
     * @return
     */
    public boolean isSelfProtectionActivated() {
        HeartbeatCounter heartbeatMeasuredRate = HeartbeatCounter.getInstance();
        if (heartbeatMeasuredRate.getLastMinuteHeartbeatRate() < expectedHeartbeatThreshold * 0.85) {
            return true;
        }
        return false;
    }
}
