package com.jonnyliu.proj.register.server;

import com.jonnyliu.proj.register.commons.DeltaRegistry;
import com.jonnyliu.proj.register.commons.HeartbeatRequest;
import com.jonnyliu.proj.register.commons.HeartbeatResponse;
import com.jonnyliu.proj.register.commons.RegisterRequest;
import com.jonnyliu.proj.register.commons.RegisterResponse;
import com.jonnyliu.proj.register.commons.ServiceInstance;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 这个controller是负责接收register-client发送过来的请求的
 * 在Spring Cloud Eureka中用的组件是jersey，百度一下jersey是什么东西
 * 在国外很常用jersey，restful框架，可以接受http请求
 *
 * @author liujie
 */
public class RegisterServerController {

	private static final Logger LOGGER = LoggerFactory.getLogger(RegisterServerController.class);

	private final ServiceRegistry registry = ServiceRegistry.getInstance();

	/**
	 * 服务注册
	 *
	 * @param registerRequest 注册请求
	 * @return 注册响应
	 */
	public RegisterResponse register(RegisterRequest registerRequest) {
		RegisterResponse registerResponse = new RegisterResponse();

		try {
			ServiceInstance serviceInstance = new ServiceInstance();
			serviceInstance.setHostname(registerRequest.getHostname());
			serviceInstance.setIp(registerRequest.getIp());
			serviceInstance.setPort(registerRequest.getPort());
			serviceInstance.setInstanceId(registerRequest.getServiceInstanceId());
			serviceInstance.setServiceName(registerRequest.getServiceName());

			registry.register(serviceInstance);
			//自我保护阈值的修改
			synchronized (SelfProtectionPolicy.class) {
				SelfProtectionPolicy selfProtectionPolicy = SelfProtectionPolicy.getInstance();
				LOGGER.info("当前实例最近一分钟心跳次数为:{}", selfProtectionPolicy);
				selfProtectionPolicy.setExpectedHeartbeatRate(selfProtectionPolicy.getExpectedHeartbeatRate() + 2);
				selfProtectionPolicy
						.setExpectedHeartbeatThreshold((long) (selfProtectionPolicy.getExpectedHeartbeatRate() * 0.85));
			}
			registerResponse.setStatus(RegisterResponse.SUCCESS);
		} catch (Exception e) {
			e.printStackTrace();
			registerResponse.setStatus(RegisterResponse.FAILURE);
		}

		return registerResponse;
	}

	/**
	 * 接受心跳请求
	 *
	 * @param heartbeatRequest 心跳请求
	 * @return 心跳响应
	 */
	public HeartbeatResponse heartbeat(HeartbeatRequest heartbeatRequest) {
		HeartbeatResponse heartbeatResponse = new HeartbeatResponse();

		try {
			ServiceInstance serviceInstance = registry.getServiceInstance(
					heartbeatRequest.getServiceName(), heartbeatRequest.getServiceInstanceId());
			serviceInstance.renew();

			//记录一下每分钟的心跳次数
			HeartbeatCounter heartbeatMeasuredRate = HeartbeatCounter.getInstance();
			heartbeatMeasuredRate.increment();

			heartbeatResponse.setStatus(HeartbeatResponse.SUCCESS);
		} catch (Exception e) {
			e.printStackTrace();
			heartbeatResponse.setStatus(HeartbeatResponse.FAILURE);
		}

		return heartbeatResponse;
	}

	/**
	 * 拉取全量注册表
	 *
	 * @return
	 */
	public Map<String, Map<String, ServiceInstance>> fetchFullRegistry() {
		return registry.getFullRegistry();
	}

	/**
	 * 拉取增量注册表
	 *
	 * @return
	 */
	public DeltaRegistry fetchDeltaRegistry() {
		return registry.getDeltaRegistry();
	}

	/**
	 * 服务下线
	 *
	 * @param serviceName 服务名称
	 * @param instanceId  服务实例ID
	 */
	public void unregister(String serviceName, String instanceId) {
		registry.remove(serviceName, instanceId);
		//自我保护阈值的修改
		synchronized (SelfProtectionPolicy.class) {
			SelfProtectionPolicy selfProtectionPolicy = SelfProtectionPolicy.getInstance();
			selfProtectionPolicy.setExpectedHeartbeatRate(selfProtectionPolicy.getExpectedHeartbeatRate() - 2);
			selfProtectionPolicy
					.setExpectedHeartbeatThreshold((long) (selfProtectionPolicy.getExpectedHeartbeatRate() * 0.85));
		}
	}

}
