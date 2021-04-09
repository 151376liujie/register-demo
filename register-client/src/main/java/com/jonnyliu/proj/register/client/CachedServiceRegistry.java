package com.jonnyliu.proj.register.client;

import com.jonnyliu.proj.register.commons.ChangedType;
import com.jonnyliu.proj.register.commons.DeltaRegistry;
import com.jonnyliu.proj.register.commons.RecentlyChangedServiceInstance;
import com.jonnyliu.proj.register.commons.ServiceInstance;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 服务注册中心的客户端缓存的一个服务注册表
 *
 * @author liujie
 */
public class CachedServiceRegistry {

    private static final Logger LOGGER = LoggerFactory.getLogger(CachedServiceRegistry.class);

    /**
     * 服务注册表拉取间隔时间
     */
    private static final Long SERVICE_REGISTRY_FETCH_INTERVAL = 30 * 1000L;

    /**
     * 客户端缓存的服务注册表
     */
    private Map<String, Map<String, ServiceInstance>> registry = new HashMap<>();

    /**
     * 拉取增量注册表信息的线程
     */
    private PullDeltaRegistryWorker deltaRegistryWorker;
    /**
     * RegisterClient
     */
    private RegisterClient registerClient;
    /**
     * http通信组件
     */
    private HttpSender httpSender;

    public CachedServiceRegistry(
            RegisterClient registerClient,
            HttpSender httpSender) {
        this.registerClient = registerClient;
        this.httpSender = httpSender;
    }

    /**
     * 初始化
     */
    public void initialize() {
        PullFullRegistryWorker fullRegistryWorker = new PullFullRegistryWorker();
        fullRegistryWorker.setName("THREAD-FETCH-FULL-SERVICE-REGISTER");
        fullRegistryWorker.start();
        this.deltaRegistryWorker = new PullDeltaRegistryWorker();
        this.deltaRegistryWorker.start();
    }

    /**
     * 销毁这个组件
     */
    public void destroy() {
        this.deltaRegistryWorker.interrupt();
    }

    /**
     * 负责定时拉取注册表到本地来进行缓存(全量拉取)
     *
     * @author liujie
     */
    private class PullFullRegistryWorker extends Thread {

        @Override
        public void run() {
            try {
                registry = httpSender.fetchServiceRegistry();
            } catch (Exception e) {
                LOGGER.error("error", e);
            }
        }
    }

    /**
     * 负责定时拉取注册表到本地来进行缓存(增量拉取)
     *
     * @author liujie
     */
    private class PullDeltaRegistryWorker extends Thread {

        @Override
        public void run() {
            while (registerClient.isRunning()) {
                try {
                    Thread.sleep(SERVICE_REGISTRY_FETCH_INTERVAL);
                    /**
                     * 最近变更的服务实例列表
                     */
                    DeltaRegistry deltaRegistry = httpSender.fetchDeltaRegistry();
                    //增量注册表与本地缓存注册表合并
                    synchronized (registry) {
                        mergerDeltaRegistry(deltaRegistry.getRecentlyChangedServiceInstances());
                    }
                    // 服务端的实例总个数
                    Long serverSideServiceInstanceTotalCount = deltaRegistry.getServiceInstanceTotalCount();
                    // 客户端缓存的服务实例个数
                    Long clientSideServiceInstanceTotalCount = getClientSideServiceInstanceTotalCount();
                    //客户端和服务端的服务实例总个数不一致,则需要全量拉取
                    if (!serverSideServiceInstanceTotalCount.equals(clientSideServiceInstanceTotalCount)) {
                        registry = httpSender.fetchServiceRegistry();
                    }
                } catch (Exception e) {
                    LOGGER.error("error", e);
                }
            }
        }

        /**
         * 获取客户端缓存的服务实例总数
         *
         * @return
         */
        private Long getClientSideServiceInstanceTotalCount() {
            Long total = 0L;
            for (Map<String, ServiceInstance> instanceMap : registry.values()) {
                total += instanceMap.size();
            }
            return total;
        }

        /**
         * 增量注册表与本地注册表
         *
         * @param deltaRegistry
         */
        private void mergerDeltaRegistry(
                LinkedList<RecentlyChangedServiceInstance> recentlyChangedServiceInstances) {
            for (RecentlyChangedServiceInstance recentlyChangedServiceInstance : recentlyChangedServiceInstances) {
                String changedType = recentlyChangedServiceInstance.getChangedType();
                ServiceInstance serviceInstance = recentlyChangedServiceInstance.getServiceInstance();
                if (ChangedType.REGISTER.equals(changedType)) {
                    // 注册
                    Map<String, ServiceInstance> map = null;
                    if (registry.containsKey(serviceInstance.getServiceName())) {
                        map = registry.get(serviceInstance.getServiceName());
                        if (!map.containsKey(serviceInstance.getInstanceId())) {
                            map.put(serviceInstance.getInstanceId(), serviceInstance);
                        }
                    } else {
                        map = new HashMap<>();
                        map.put(serviceInstance.getInstanceId(), serviceInstance);
                        registry.put(serviceInstance.getServiceName(), map);
                    }
                } else if (ChangedType.REMOVE.equals(serviceInstance.getServiceName())) {
                    if (registry.containsKey(serviceInstance.getServiceName())) {
                        registry.get(serviceInstance.getServiceName()).remove(serviceInstance.getInstanceId());
                    }
                }
            }
        }
    }

    /**
     * 获取服务注册表
     *
     * @return
     */
    public Map<String, Map<String, ServiceInstance>> getRegistry() {
        return registry;
    }

}
