package com.jonnyliu.proj.register.client;

import com.jonnyliu.proj.register.commons.DeltaRegistry;
import com.jonnyliu.proj.register.commons.HeartbeatRequest;
import com.jonnyliu.proj.register.commons.HeartbeatResponse;
import com.jonnyliu.proj.register.commons.RecentlyChangedServiceInstance;
import com.jonnyliu.proj.register.commons.RegisterRequest;
import com.jonnyliu.proj.register.commons.RegisterResponse;
import com.jonnyliu.proj.register.commons.ServiceInstance;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 负责发送各种http请求的组件
 *
 * @author liujie
 */
public class HttpSender {

    private static final Logger log = LoggerFactory.getLogger(HttpSender.class);

	/**
	 * 发送注册请求
	 *
	 * @param request 注册请求
	 * @return 注册响应
	 */
	public RegisterResponse register(RegisterRequest request) {
		// 实际上会基于类似HttpClient这种开源的网络包
		// 你可以去构造一个请求，里面放入这个服务实例的信息，比如服务名称，ip地址，端口号
		// 然后通过这个请求发送过去
        log.info("服务实例【{}】，发送请求进行注册......", request);

		// 收到register-server响应之后，封装一个Response对象
		RegisterResponse response = new RegisterResponse();
		response.setStatus(RegisterResponse.SUCCESS);

		return response;
	}

    /**
     * 发送心跳请求
     *
     * @param request 心跳请求
     * @return 心跳响应
     */
    public HeartbeatResponse heartbeat(HeartbeatRequest request) {
        log.info("服务实例【{}】，发送请求进行心跳......", request.getServiceInstanceId());

        HeartbeatResponse response = new HeartbeatResponse();
        response.setStatus(RegisterResponse.SUCCESS);

        return response;
    }

    /**
     * 拉取全量注册表信息
     *
     * @return 全量注册表信息
     */
    public Map<String, Map<String, ServiceInstance>> fetchFullServiceRegistry() {
        Map<String, Map<String, ServiceInstance>> registry =
                new ConcurrentHashMap<>();

        ServiceInstance serviceInstance = new ServiceInstance();
        serviceInstance.setHostname("finance-service-01");
        serviceInstance.setIp("192.168.31.207");
        serviceInstance.setPort(9000);
        serviceInstance.setInstanceId("FINANCE-SERVICE-192.168.31.207:9000");
        serviceInstance.setServiceName("FINANCE-SERVICE");

        Map<String, ServiceInstance> serviceInstances = new ConcurrentHashMap<>();
        serviceInstances.put("FINANCE-SERVICE-192.168.31.207:9000", serviceInstance);

        registry.put("FINANCE-SERVICE", serviceInstances);

        log.info("拉取全量注册表：{}", registry);

        return registry;
    }

    /**
     * 增量拉取服务注册表
     *
     * @return 增量注册表
     */
    public DeltaRegistry fetchDeltaServiceRegistry() {
        LinkedList<RecentlyChangedServiceInstance> recentlyChangedQueue =
                new LinkedList<>();

        ServiceInstance serviceInstance = new ServiceInstance();
        serviceInstance.setHostname("order-service-01");
        serviceInstance.setIp("192.168.31.288");
        serviceInstance.setPort(9000);
        serviceInstance.setInstanceId("ORDER-SERVICE-192.168.31.288:9000");
        serviceInstance.setServiceName("ORDER-SERVICE");

        RecentlyChangedServiceInstance recentlyChangedItem = new RecentlyChangedServiceInstance(
                serviceInstance,
                "register");

        recentlyChangedQueue.add(recentlyChangedItem);

        log.info("拉取增量注册表：{}", recentlyChangedQueue);

        return new DeltaRegistry(recentlyChangedQueue, 2L);
    }

    /**
     * 服务下线
     *
     * @param serviceName 服务名称
     * @param instanceId  服务实例ID
     */
    public void cancel(String serviceName, String instanceId) {
        log.info("服务实例下线：serviceName: [{}], 实例ID: [{}]", serviceName, instanceId);
    }
}