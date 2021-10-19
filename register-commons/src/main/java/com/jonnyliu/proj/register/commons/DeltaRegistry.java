package com.jonnyliu.proj.register.commons;

import java.util.LinkedList;

/**
 * 增量注册表信息
 *
 * @author liujie
 */
public class DeltaRegistry {

    /**
     * 最近变更的服务实例队列
     */
    private final LinkedList<RecentlyChangedServiceInstance> recentlyChangedServiceInstances;
    /**
     * 服务端的实例总数,用于纠正客户端缓存的注册信息
     */
    private final Long serviceInstanceTotalCount;

    public DeltaRegistry(
            LinkedList<RecentlyChangedServiceInstance> recentlyChangedServiceInstances,
            Long serviceInstanceTotalCount) {
        this.recentlyChangedServiceInstances = recentlyChangedServiceInstances;
        this.serviceInstanceTotalCount = serviceInstanceTotalCount;
    }

    public LinkedList<RecentlyChangedServiceInstance> getRecentlyChangedServiceInstances() {
        return recentlyChangedServiceInstances;
    }

    public Long getServiceInstanceTotalCount() {
        return serviceInstanceTotalCount;
    }

}