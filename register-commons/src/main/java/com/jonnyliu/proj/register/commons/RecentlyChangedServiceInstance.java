package com.jonnyliu.proj.register.commons;

import java.util.StringJoiner;

/**
 * 最近变更的服务实例
 *
 * @author liujie
 */
public class RecentlyChangedServiceInstance {

    /**
     * 服务实例
     */
    private ServiceInstance serviceInstance;

    /**
     * 发生变更的时间戳
     */
    private Long changedTimestamp;

    /**
     * 变更类型
     */
    private String changedType;

    public RecentlyChangedServiceInstance(ServiceInstance serviceInstance, String changedType) {
        this.changedTimestamp = System.currentTimeMillis();
        this.serviceInstance = serviceInstance;
        this.changedType = changedType;
    }

    public ServiceInstance getServiceInstance() {
        return serviceInstance;
    }

    public Long getChangedTimestamp() {
        return changedTimestamp;
    }

    public String getChangedType() {
        return changedType;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", RecentlyChangedServiceInstance.class.getSimpleName() + "[", "]")
                .add("serviceInstance=" + serviceInstance)
                .add("changedTimestamp=" + changedTimestamp)
                .add("changedType='" + changedType + "'")
                .toString();
    }
}