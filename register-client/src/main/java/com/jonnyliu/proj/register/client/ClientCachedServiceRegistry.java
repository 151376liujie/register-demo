package com.jonnyliu.proj.register.client;

import com.jonnyliu.proj.register.commons.Applications;
import com.jonnyliu.proj.register.commons.ChangedType;
import com.jonnyliu.proj.register.commons.DeltaRegistry;
import com.jonnyliu.proj.register.commons.RecentlyChangedServiceInstance;
import com.jonnyliu.proj.register.commons.ServiceInstance;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.atomic.AtomicStampedReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 服务注册中心的客户端缓存的一个服务注册表
 *
 * @author liujie
 */
public class ClientCachedServiceRegistry {

    private static final Logger log = LoggerFactory.getLogger(ClientCachedServiceRegistry.class);

    /**
     * 服务注册表拉取间隔时间
     */
    private static final Long SERVICE_REGISTRY_FETCH_INTERVAL = 30 * 1000L;
    private static final String THREAD_FETCH_FULL_SERVICE_REGISTER = "THREAD-FETCH-FULL-SERVICE-REGISTER";
    private static final String THREAD_FETCH_DELTA_SERVICE_REGISTER = "THREAD-FETCH-DELTA-SERVICE-REGISTER";

    /**
     * 客户端缓存的服务注册表
     */
    private AtomicStampedReference<Applications> apps = new AtomicStampedReference<>(new Applications(), 0);

    /**
     * 拉取增量注册表信息的线程
     */
    private FetchDeltaRegistryWorker deltaRegistryWorker;
    /**
     * RegisterClient
     */
    private RegisterClient registerClient;
    /**
     * http通信组件
     */
    private HttpSender httpSender;

    public ClientCachedServiceRegistry(RegisterClient registerClient, HttpSender httpSender) {
        this.registerClient = registerClient;
        this.httpSender = httpSender;
    }

    /**
     * 初始化
     */
    public void initialize() {
        //抓取全量注册表工作线程
        FetchFullRegistryWorker fullRegistryWorker = new FetchFullRegistryWorker(THREAD_FETCH_FULL_SERVICE_REGISTER);
        fullRegistryWorker.start();

        //抓取增量注册表工作线程
        this.deltaRegistryWorker = new FetchDeltaRegistryWorker(THREAD_FETCH_DELTA_SERVICE_REGISTER);
        this.deltaRegistryWorker.start();
    }

    /**
     * 销毁这个组件
     */
    public void destroy() {
        this.deltaRegistryWorker.interrupt();
    }

    /**
     * 获取服务注册表
     *
     * @return
     */
    public Map<String, Map<String, ServiceInstance>> getRegistry() {
        return apps.getReference().getRegistry();
    }

    /**
     * 负责定时拉取注册表到本地来进行缓存(全量拉取)
     *
     * @author liujie
     */
    private class FetchFullRegistryWorker extends Thread {

        public FetchFullRegistryWorker(String name) {
            super(name);
        }

        @Override
        public void run() {
            try {
                if (registerClient.isRunning()) {
                    Applications newValue = httpSender.fetchFullServiceRegistry();
                    while (true) {
                        Applications expected = apps.getReference();
                        int stamp = apps.getStamp();
                        if (apps.compareAndSet(expected, newValue, stamp, stamp + 1)) {
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                log.error("fetch full service registry error", e);
            }
        }
    }

    /**
     * 负责定时拉取注册表到本地来进行缓存(增量拉取)
     *
     * @author liujie
     */
    private class FetchDeltaRegistryWorker extends Thread {

        public FetchDeltaRegistryWorker(String name) {
            super(name);
        }

        @Override
        public void run() {
            while (registerClient.isRunning()) {
                try {
                    Thread.sleep(SERVICE_REGISTRY_FETCH_INTERVAL);
                    /**
                     * 最近变更的服务实例列表
                     */
                    DeltaRegistry deltaRegistry = httpSender.fetchDeltaServiceRegistry();
                    //增量注册表与本地缓存注册表合并
                    mergerDeltaRegistry(deltaRegistry);
                    //调整注册表
                    reconcileRegistryIfNecessary(deltaRegistry);
                } catch (Exception e) {
                    log.error("error", e);
                }
            }
        }

        /**
         * 调整注册表
         *
         * @param deltaRegistry 增量注册表
         */
        private void reconcileRegistryIfNecessary(DeltaRegistry deltaRegistry) {
            // 服务端的实例总个数
            Long serverSideServiceInstanceTotalCount = deltaRegistry.getServiceInstanceTotalCount();
            // 客户端缓存的服务实例个数
            Long clientSideServiceInstanceTotalCount = getClientSideServiceInstanceTotalCount();
            //客户端和服务端的服务实例总个数不一致,则需要全量拉取
            if (!serverSideServiceInstanceTotalCount.equals(clientSideServiceInstanceTotalCount)) {
                log.info(
                        "fetch delta registry, client side instance count: {} not equals server side instance count: {}, prepare to fetch full registry",
                        clientSideServiceInstanceTotalCount, serverSideServiceInstanceTotalCount);
                Applications fetchedRegistry = httpSender.fetchFullServiceRegistry();
                while (true) {
                    Applications expected = apps.getReference();
                    int expectedStamp = apps.getStamp();
                    if (apps.compareAndSet(expected, fetchedRegistry, expectedStamp, expectedStamp + 1)) {
                        break;
                    }
                }
            }
        }

        /**
         * 获取客户端缓存的服务实例总数
         *
         * @return 客户端缓存的服务实例总数
         */
        private Long getClientSideServiceInstanceTotalCount() {
            Long total = 0L;
            for (Map<String, ServiceInstance> instanceMap : apps.getReference().getRegistry().values()) {
                total += instanceMap.size();
            }
            return total;
        }

        /**
         * 增量注册表与本地注册表
         *
         * @param deltaRegistry 增量注册表
         */
        private void mergerDeltaRegistry(DeltaRegistry deltaRegistry) {
            synchronized (apps) {
                LinkedList<RecentlyChangedServiceInstance> recentlyChangedServiceInstances =
                        deltaRegistry.getRecentlyChangedServiceInstances();
                for (RecentlyChangedServiceInstance recentlyChangedServiceInstance : recentlyChangedServiceInstances) {
                    String changedType = recentlyChangedServiceInstance.getChangedType();
                    ServiceInstance serviceInstance = recentlyChangedServiceInstance.getServiceInstance();
                    Map<String, Map<String, ServiceInstance>> registry = apps.getReference().getRegistry();
                    if (ChangedType.REGISTER.equals(changedType)) {
                        // 注册
                        Map<String, ServiceInstance> serviceInstanceMap = registry.get(
                                serviceInstance.getServiceName());
                        if (serviceInstanceMap == null) {
                            serviceInstanceMap = new HashMap<>();
                        }
                        serviceInstanceMap.put(serviceInstance.getInstanceId(), serviceInstance);
                        registry.put(serviceInstance.getServiceName(), serviceInstanceMap);
                    } else if (ChangedType.REMOVE.equals(serviceInstance.getServiceName())) {
                        if (registry.containsKey(serviceInstance.getServiceName())) {
                            registry.get(serviceInstance.getServiceName())
                                    .remove(serviceInstance.getInstanceId());
                        }
                    }
                }
            }
        }
    }
}