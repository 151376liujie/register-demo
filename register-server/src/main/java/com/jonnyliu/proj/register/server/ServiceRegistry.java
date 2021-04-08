package com.jonnyliu.proj.register.server;

import java.util.HashMap;
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

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceRegistry.class);

    private static final Map<String, Map<String, ServiceInstance>> REGISTER_MAP = new ConcurrentHashMap<>();
    private static final ServiceRegistry INSTANCE = new ServiceRegistry();

    private ServiceRegistry() {
    }

    public static ServiceRegistry getInstance() {
        return INSTANCE;
    }

    /**
     * 服务实例注册
     *
     * @param serviceInstance 服务实例
     */
    public synchronized void register(ServiceInstance serviceInstance) {
        LOGGER.info("注册服务,服务名称:[{}], 服务实例ID: [{}] ", serviceInstance.getServiceName(), serviceInstance.getInstanceId());
        Map<String, ServiceInstance> serviceInstanceMap;
        if (REGISTER_MAP.containsKey(serviceInstance.getServiceName())) {
            serviceInstanceMap = REGISTER_MAP.get(serviceInstance.getServiceName());
        } else {
            serviceInstanceMap = new HashMap<>();
        }
        serviceInstanceMap.put(serviceInstance.getInstanceId(), serviceInstance);
        REGISTER_MAP.put(serviceInstance.getServiceName(), serviceInstanceMap);
    }

    /**
     * 删除指定服务名称的某个服务实例
     *
     * @param instance 服务实例
     */
    public synchronized void remove(String serviceName, String instanceId) {
        LOGGER.info("服务名称:[{}],服务实例ID: [{}]从注册中心被摘除", serviceName, instanceId);
        if (!REGISTER_MAP.containsKey(serviceName)) {
            return;
        }
        Map<String, ServiceInstance> serviceInstances = REGISTER_MAP.get(serviceName);
        serviceInstances.remove(instanceId);
    }

    /**
     * 获取注册表
     *
     * @return 获取注册表
     */
    public synchronized Map<String, Map<String, ServiceInstance>> getRegistry() {
        return REGISTER_MAP;
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
}
