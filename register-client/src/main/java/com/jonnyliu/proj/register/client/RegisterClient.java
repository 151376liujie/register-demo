package com.jonnyliu.proj.register.client;

import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 在服务上被创建和启动，负责跟register-server进行通信
 *
 * @author liujie
 */
public class RegisterClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(RegisterClient.class);

    public static final String SERVICE_NAME = "inventory-service";
    public static final String IP = "192.168.31.207";
    public static final String HOSTNAME = "inventory01";
    public static final int PORT = 9000;

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
    private CachedServiceRegistry registry;

    /**
     * 服务实例id
     */
    private String serviceInstanceId;

    public RegisterClient() {
        this.serviceInstanceId = UUID.randomUUID().toString().replace("-", "");
        this.httpSender = new HttpSender();
        this.isRunning = true;
        this.heartbeatWorker = new HeartbeatWorker("THREAD-HEARTBEAT");
        this.registry = new CachedServiceRegistry(this, httpSender);
    }

    public void start() throws InterruptedException {
        // 一旦启动了这个组件之后，他就负责在服务上干两个事情
        // 第一个事情，就是开启一个线程向register-server去发送请求，注册这个服务
        // 第二个事情，就是在注册成功之后，就会开启另外一个线程去发送心跳

        // 我们来简化一下这个模型
        // 我们在register-client这块就开启一个线程
        // 这个线程刚启动的时候，第一个事情就是完成注册
        // 如果注册完成了之后，他就会进入一个while true死循环
        // 每隔30秒就发送一个请求去进行心跳

        RegisterClientWorker registerClientWorker = new RegisterClientWorker("THREAD-REGISTER-WORKER");
        registerClientWorker.start();
        //等待服务注册完成
        registerClientWorker.join();
        //开启心跳线程
        heartbeatWorker.start();
        //开启定时拉取服务注册表线程
        this.registry.initialize();
    }

    public void setRunning(boolean running) {
        this.isRunning = running;
    }

    public void shutdown() {
        setRunning(false);
        heartbeatWorker.interrupt();
        registry.destroy();
        unregister();
    }

    /**
     * 通知服务下线
     */
    public void unregister() {
        httpSender.unregister(SERVICE_NAME, serviceInstanceId);
    }

    public static void main(String[] args) throws InterruptedException {
        RegisterClient registerClient = new RegisterClient();
        registerClient.start();

        Thread.sleep(30 * 1000);
        registerClient.shutdown();
    }

    public boolean isRunning() {
        return this.isRunning;
    }

    private class RegisterClientWorker extends Thread {

        public RegisterClientWorker(String name) {
            super(name);
        }

        @Override
        public void run() {
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

            LOGGER.info("服务注册的结果是：" + registerResponse.getStatus() + "......");
        }
    }

    /**
     * 心跳工作线程
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
            HeartbeatResponse heartbeatResponse = null;

            while (isRunning) {
                try {
                    heartbeatResponse = httpSender.heartbeat(heartbeatRequest);
                    LOGGER.info("心跳的结果为：" + heartbeatResponse.getStatus() + "......");
                    Thread.sleep(30 * 1000);
                } catch (Exception e) {
                    LOGGER.error("error", e);
                }
            }
        }
    }

}
