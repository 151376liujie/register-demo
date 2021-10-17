package com.jonnyliu.proj.register.server;


import com.jonnyliu.proj.register.commons.ServiceInstance;
import java.util.Map;
import java.util.Map.Entry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 检测服务是否存活的后台线程
 *
 * @author liujie
 */
public class ServiceAliveMonitor {

    private static final Logger log = LoggerFactory.getLogger(ServiceAliveMonitor.class);

    private static final long CHECK_ALIVE_INTERVAL = 60 * 1000;

    private final Daemon daemon;

    public ServiceAliveMonitor() {
        this.daemon = new Daemon();
        this.daemon.setDaemon(true);
        this.daemon.setName("THREAD-SERVICE-ALIVE-MONITOR");
    }

    public void start() {
        daemon.start();
    }

    /**
     * 检验服务是否存活的后台线程
     */
    private class Daemon extends Thread {

        private final ServiceRegistry registry = ServiceRegistry.getInstance();

        @Override
        public void run() {
            SelfProtectionPolicy selfProtectionPolicy = SelfProtectionPolicy.getInstance();

            while (true) {
                try {
                    //判断是否进入自我保护机制
                    if (selfProtectionPolicy.isSelfProtectionActivated()) {
                        log.warn("注册中心进入自我保护机制....");
                        Thread.sleep(CHECK_ALIVE_INTERVAL);
                        continue;
                    }
                    Map<String, Map<String, ServiceInstance>> fullRegistry = registry.getFullRegistry();
                    for (Entry<String, Map<String, ServiceInstance>> entry : fullRegistry.entrySet()) {
                        log.info("服务:{},实例个数：>>>>>> {}", entry.getKey(), entry.getValue().size());
                        Map<String, ServiceInstance> instanceMap = entry.getValue();
                        for (Entry<String, ServiceInstance> instanceEntry : instanceMap.entrySet()) {
                            ServiceInstance instance = instanceEntry.getValue();
                            if (!instance.isAlive()) {
                                registry.remove(instance.getServiceName(), instance.getInstanceId());
                                //自我保护阈值的修改
                                synchronized (SelfProtectionPolicy.class) {
                                    selfProtectionPolicy.setExpectedHeartbeatRate(
                                            selfProtectionPolicy.getExpectedHeartbeatRate() - 2);
                                    selfProtectionPolicy.setExpectedHeartbeatThreshold(
                                            (long) (selfProtectionPolicy.getExpectedHeartbeatRate() * 0.85));
                                }
                            }
                        }
                    }
                    Thread.sleep(CHECK_ALIVE_INTERVAL);
                } catch (Exception e) {
                    log.info("线程:[{}],被打断......", Thread.currentThread().getName());
                }
            }
        }
    }
}