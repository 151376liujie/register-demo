package com.jonnyliu.proj.register.client;

import com.jonnyliu.proj.register.commons.HeartbeatRequest;
import com.jonnyliu.proj.register.commons.HeartbeatResponse;
import com.jonnyliu.proj.register.commons.RegisterRequest;
import com.jonnyliu.proj.register.commons.RegisterResponse;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 在服务上被创建和启动，负责跟register-server进行通信
 *
 * @author liujie
 */
public class RegisterClient {

    private static final Logger log = LoggerFactory.getLogger(RegisterClient.class);

    private static final String SERVICE_NAME = "inventory-service";
    private static final String IP = "192.168.31.207";
    private static final String HOSTNAME = "inventory01";
    private static final int PORT = 9000;
    private static final Long HEARTBEAT_INTERVAL = 30 * 1000L;

    /**
     * http通信组件
     */
    private HttpSender httpSender;
    /**
     * 是否运行的标志位
     */
    private volatile boolean isRunning;

    /**
     * 心跳线程
     */
    private HeartbeatWorker heartbeatWorker;
    /**
     * 客户端缓存的服务注册表
     */
    private ClientCachedServiceRegistry registry;

    /**
     * 服务实例id
     */
    private String serviceInstanceId;

    public RegisterClient() {
        this.serviceInstanceId = UUID.randomUUID().toString().replace("-", "");
        this.httpSender = new HttpSender();
        this.heartbeatWorker = new HeartbeatWorker("THREAD-HEARTBEAT");
        this.registry = new ClientCachedServiceRegistry(this, httpSender);
    }

    /**
     * 服务下线
     */
    public void shutdown() {
        setRunning(false);
        heartbeatWorker.interrupt();
        registry.destroy();
        unregister();
    }

    /**
     * 通知服务下线
     */
    private void unregister() {
        httpSender.cancel(SERVICE_NAME, serviceInstanceId);
    }

    /**
     * 组件启动， 主要是启动服务注册线程、心跳发送线程 & 初始化注册中心客户端缓存的注册表组件
     *
     * @throws Exception 异常
     */
    public void start() throws Exception {
        // 一旦启动了这个组件之后，他就负责在服务上干两个事情
        // 第一个事情，就是开启一个线程向register-server去发送请求，注册这个服务
        // 第二个事情，就是在注册成功之后，就会开启另外一个线程去发送心跳

        // 我们来简化一下这个模型
        // 我们在register-client这块就开启一个线程
        // 这个线程刚启动的时候，第一个事情就是完成注册
        // 如果注册完成了之后，他就会进入一个while true死循环
        // 每隔30秒就发送一个请求去进行心跳

        setRunning(true);

        RegisterWorker registerWorker = new RegisterWorker("THREAD-REGISTER-WORKER");
        registerWorker.start();
        //等待服务注册完成
        registerWorker.join();
        //开启心跳线程
        heartbeatWorker.start();
        //开启定时拉取服务注册表线程
        this.registry.initialize();
    }

    /**
     * 获取服务注册中心客户端的运行状态标志位
     *
     * @return 运行状态标志位
     */
    public boolean isRunning() {
        return this.isRunning;
    }

    /**
     * 设置注册中心客户端的运行状态标志位
     *
     * @param running 运行状态标志位
     */
    public void setRunning(boolean running) {
        this.isRunning = running;
    }

    /**
     * 服务注册工作线程
     */
    private class RegisterWorker extends Thread {

        public RegisterWorker(String name) {
            super(name);
        }

        @Override
        public void run() {
            if (isRunning()) {
                // 应该是获取当前机器的信息
                // 包括当前机器的ip地址、hostname，以及你配置这个服务监听的端口号
                // 从配置文件里可以拿到
                RegisterRequest registerRequest = new RegisterRequest();
                registerRequest.setServiceName(SERVICE_NAME);
                registerRequest.setIp(IP);
                registerRequest.setHostname(HOSTNAME);
                registerRequest.setPort(PORT);
                registerRequest.setServiceInstanceId(serviceInstanceId);

                RegisterResponse registerResponse = httpSender.register(registerRequest);

                log.info("服务：{}注册的结果是：{}", SERVICE_NAME + ":" + serviceInstanceId, registerResponse.getStatus());
            }
        }
    }

    /**
     * 定时发送心跳工作线程
     */
    private class HeartbeatWorker extends Thread {

        public HeartbeatWorker(String name) {
            super(name);
        }

        @Override
        public void run() {
            HeartbeatRequest heartbeatRequest = new HeartbeatRequest();
            heartbeatRequest.setServiceInstanceId(serviceInstanceId);
            heartbeatRequest.setServiceName(SERVICE_NAME);

            while (isRunning) {
                try {
                    HeartbeatResponse heartbeatResponse = httpSender.heartbeat(heartbeatRequest);
                    log.info("服务:{}的心跳的结果为：{}", SERVICE_NAME + ":" + serviceInstanceId,
                            heartbeatResponse.getStatus() + "......");
                    Thread.sleep(HEARTBEAT_INTERVAL);
                } catch (Exception e) {
                    log.error("send heartbeat error", e);
                }
            }
        }
    }
}