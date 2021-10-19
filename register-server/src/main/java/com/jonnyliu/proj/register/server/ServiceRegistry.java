package com.jonnyliu.proj.register.server;

import com.jonnyliu.proj.register.commons.ChangedType;
import com.jonnyliu.proj.register.commons.DeltaRegistry;
import com.jonnyliu.proj.register.commons.RecentlyChangedServiceInstance;
import com.jonnyliu.proj.register.commons.ServiceInstance;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 服务注册中心,他是一个单例
 *
 * @author liujie
 */
public class ServiceRegistry {

    private static final Logger log = LoggerFactory.getLogger(ServiceRegistry.class);

    public static final Long RECENTLY_CHANGED_ITEM_CHECK_INTERVAL = 3 * 1000L;
    public static final Long RECENTLY_CHANGED_ITEM_EXPIRED = 3 * 60 * 1000L;
    private static final String RECENTLY_CHANGED_SERVICE_INSTANCE_MONITOR = "RECENTLY_CHANGED_SERVICE_INSTANCE_MONITOR";


    /**
     * 核心的内存数据结构：注册表
     * <p>
     * Map：key是服务名称，value是这个服务的所有的服务实例
     * Map<String, ServiceInstance>：key是服务实例id，value是服务实例的信息
     */
    private static final Map<String, Map<String, ServiceInstance>> REGISTER_MAP = new ConcurrentHashMap<>();

    /**
     * 最近变更的服务实例的队列
     */
    private LinkedList<RecentlyChangedServiceInstance> recentlyChangedQueue = new LinkedList<>();

    /**
     * 注册表是个单例
     */
    private static final ServiceRegistry INSTANCE = new ServiceRegistry();

    private ServiceRegistry() {
        //启动后台线程监控最近变更的队列
        RecentlyChangedQueueMonitor recentlyChangedQueueMonitor = new RecentlyChangedQueueMonitor(
                RECENTLY_CHANGED_SERVICE_INSTANCE_MONITOR);
        recentlyChangedQueueMonitor.setDaemon(true);
        recentlyChangedQueueMonitor.start();
    }

    public static ServiceRegistry getInstance() {
        return INSTANCE;
    }

    /**
     * 服务实例个数
     */
    private Long getServiceInstanceTotalCount() {
        long total = 0L;
        for (Map<String, ServiceInstance> map : REGISTER_MAP.values()) {
            total += map.size();
        }
        return total;
    }

    /**
     * 服务实例注册
     *
     * @param serviceInstance 服务实例
     */
    public synchronized void register(ServiceInstance serviceInstance) {
        log.info("注册服务,服务名称:[{}], 服务实例ID: [{}] ", serviceInstance.getServiceName(), serviceInstance.getInstanceId());
        Map<String, ServiceInstance> serviceInstanceMap;
        if (!REGISTER_MAP.containsKey(serviceInstance.getServiceName())) {
            serviceInstanceMap = new ConcurrentHashMap<>();
        } else {
            serviceInstanceMap = REGISTER_MAP.get(serviceInstance.getServiceName());
        }
        serviceInstanceMap.put(serviceInstance.getInstanceId(), serviceInstance);
        REGISTER_MAP.put(serviceInstance.getServiceName(), serviceInstanceMap);

        //将新注册的服务实例加入最近变更的服务实例队列中区
        addRecentlyChangedQueue(serviceInstance, ChangedType.REGISTER);
    }

    /**
     * 删除指定服务名称的某个服务实例
     *
     * @param serviceName 服务名称
     * @param instanceId  服务实例id
     */
    public synchronized void remove(String serviceName, String instanceId) {
        log.info("服务名称:[{}],服务实例ID: [{}]从注册中心被摘除", serviceName, instanceId);
        if (!REGISTER_MAP.containsKey(serviceName)) {
            return;
        }
        Map<String, ServiceInstance> serviceInstances = REGISTER_MAP.get(serviceName);

        ServiceInstance serviceInstance = serviceInstances.remove(instanceId);
        //添加服务实例到最近变更的服务实例队列中
        addRecentlyChangedQueue(serviceInstance, ChangedType.REMOVE);
    }

    /**
     * 将最近变更的服务实例加入最近变更服务实例队列中去
     *
     * @param serviceInstance 服务实例
     * @param changedType     变更类型
     */
    private void addRecentlyChangedQueue(ServiceInstance serviceInstance, String changedType) {
        RecentlyChangedServiceInstance changedServiceInstance = new RecentlyChangedServiceInstance(serviceInstance,
                changedType);
        recentlyChangedQueue.offer(changedServiceInstance);
    }

    /**
     * 获取全量注册表
     *
     * @return 获取注册表
     */
    public synchronized Map<String, Map<String, ServiceInstance>> getFullRegistry() {
        return REGISTER_MAP;
    }

    /**
     * 获取增量注册表
     *
     * @return
     */
    public synchronized DeltaRegistry getDeltaRegistry() {
        Long totalCount = getServiceInstanceTotalCount();
        return new DeltaRegistry(recentlyChangedQueue, totalCount);
    }

    /**
     * 获取指定服务名称的指定服务实例信息
     *
     * @param serviceName       服务名称
     * @param serviceInstanceId 服务实例
     * @return 服务实例信息
     */
    public synchronized ServiceInstance getServiceInstance(String serviceName, String serviceInstanceId) {
        Map<String, ServiceInstance> serviceInstances = REGISTER_MAP.get(serviceName);
        return serviceInstances.get(serviceInstanceId);
    }

    /**
     * 最近变更的服务实例队列监控线程,将队列中服务实例的最近变更时间大于3分钟的实例移除
     */
    class RecentlyChangedQueueMonitor extends Thread {

        public RecentlyChangedQueueMonitor(String name) {
            super(name);
        }

        @Override
        public void run() {
            while (true) {
                try {
                    synchronized (INSTANCE) {
                        RecentlyChangedServiceInstance recentlyChangedItem = null;
                        Long currentTimestamp = System.currentTimeMillis();
                        while ((recentlyChangedItem = recentlyChangedQueue.peek()) != null) {
                            // 判断如果一个服务实例变更信息已经在队列里存在超过3分钟了
                            // 就从队列中移除
                            if (currentTimestamp - recentlyChangedItem.getChangedTimestamp()
                                    > RECENTLY_CHANGED_ITEM_EXPIRED) {
                                recentlyChangedQueue.pop();
                            } else {
                                break;
                            }
                        }
                    }
                    Thread.sleep(RECENTLY_CHANGED_ITEM_CHECK_INTERVAL);
                } catch (Exception e) {
                    log.error("error.", e);
                }
            }
        }
    }

}