package com.jonnyliu.proj.register.server;

import com.jonnyliu.proj.register.commons.HeartbeatRequest;
import com.jonnyliu.proj.register.commons.RegisterRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.Test;

public class RegistryServerTest {

    @Test
    public void test() throws InterruptedException {
        RegisterServerController registerServerController = new RegisterServerController();

        List<RegisterRequest> list = new ArrayList<>();

        //模拟注册服务到注册中心
        for (int i = 0; i < 1; i++) {
            RegisterRequest registerRequest = new RegisterRequest();
            registerRequest.setHostname("inventory-service-" + i);
            registerRequest.setIp("192.168.16." + i);
            registerRequest.setPort(9000 + i);
            String instanceId = UUID.randomUUID().toString().replace("-", "");
            registerRequest.setServiceInstanceId(instanceId);
            registerRequest.setServiceName("inventory-service");
            registerServerController.register(registerRequest);
            list.add(registerRequest);
        }

        for (int i = 0; i < 1; i++) {
            RegisterRequest registerRequest = list.get(i);
            // 模拟进行一次心跳，完成续约
            HeartbeatRequest heartbeatRequest = new HeartbeatRequest();
            heartbeatRequest.setServiceInstanceId(registerRequest.getServiceInstanceId());
            heartbeatRequest.setServiceName(registerRequest.getServiceName());
            registerServerController.heartbeat(heartbeatRequest);
        }

        // 开启一个后台线程，检测微服务的存活状态
        ServiceAliveMonitor serviceAliveMonitor = new ServiceAliveMonitor();
        serviceAliveMonitor.start();
        while (true) {
            Thread.sleep(30 * 1000);
        }
    }
}