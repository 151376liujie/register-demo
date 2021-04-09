package com.jonnyliu.proj.register.client;

import com.jonnyliu.proj.register.commons.DeltaRegistry;
import com.jonnyliu.proj.register.commons.HeartbeatRequest;
import com.jonnyliu.proj.register.commons.HeartbeatResponse;
import com.jonnyliu.proj.register.commons.RecentlyChangedServiceInstance;
import com.jonnyliu.proj.register.commons.RegisterRequest;
import com.jonnyliu.proj.register.commons.RegisterResponse;
import com.jonnyliu.proj.register.commons.ServiceInstance;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 负责发送各种http请求的组件
 *
 * @author liujie
 */
public class HttpSender {

	private static final Logger LOGGER = LoggerFactory.getLogger(HttpSender.class);

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
		LOGGER.info("服务实例【" + request + "】，发送请求进行注册......");

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
		LOGGER.info("服务实例【" + request.getServiceInstanceId() + "】，发送请求进行心跳......");

		HeartbeatResponse response = new HeartbeatResponse();
		response.setStatus(RegisterResponse.SUCCESS);

		return response;
	}

	public Map<String, Map<String, ServiceInstance>> fetchServiceRegistry() {
		Map<String, Map<String, ServiceInstance>> registry =
				new HashMap<>();

		ServiceInstance serviceInstance = new ServiceInstance();
		serviceInstance.setHostname("finance-service-01");
		serviceInstance.setIp("192.168.31.1207");
		serviceInstance.setPort(9000);
		serviceInstance.setInstanceId("FINANCE-SERVICE-192.168.31.207:9000");
		serviceInstance.setServiceName("FINANCE-SERVICE");

		Map<String, ServiceInstance> serviceInstances = new HashMap<>();
		serviceInstances.put("FINANCE-SERVICE-192.168.31.207:9000", serviceInstance);

		registry.put("FINANCE-SERVICE", serviceInstances);

		LOGGER.info("拉取注册表：" + registry);

		return registry;
	}

	/**
	 * 增量拉取服务注册表
	 *
	 * @return
	 */
	public DeltaRegistry fetchDeltaRegistry() {
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

		LOGGER.info("拉取增量注册表：{}", recentlyChangedQueue);

		return new DeltaRegistry(recentlyChangedQueue, 2L);
	}

	public void unregister(String serviceName, String instanceId) {
		LOGGER.info("服务实例下线：serviceName: [{}], 实例ID: [{}]", serviceName, instanceId);
	}
}
