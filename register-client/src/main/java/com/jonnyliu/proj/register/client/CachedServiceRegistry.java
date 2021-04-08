package com.jonnyliu.proj.register.client;

import java.util.HashMap;
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
	private Map<String, Map<String, ServiceInstance>> registry =
			new HashMap<>();
	/**
	 * 负责定时拉取注册表到客户端进行缓存的后台线程
	 */
	private Daemon daemon;
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
		this.daemon = new Daemon();
		this.registerClient = registerClient;
		this.httpSender = httpSender;
	}

	/**
	 * 初始化
	 */
	public void initialize() {
		this.daemon.setName("THREAD-FETCH-SERVICE-REGISTER");
		this.daemon.start();
	}

	/**
	 * 销毁这个组件
	 */
	public void destroy() {
		this.daemon.interrupt();
	}

	/**
	 * 负责定时拉取注册表到本地来进行缓存
	 *
	 * @author liujie
	 */
	private class Daemon extends Thread {

		@Override
		public void run() {
			while (registerClient.isRunning()) {
				try {
					registry = httpSender.fetchServiceRegistry();
					Thread.sleep(SERVICE_REGISTRY_FETCH_INTERVAL);
				} catch (Exception e) {
					LOGGER.error("error", e);
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
