package com.jonnyliu.proj.register.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServiceInstance {

    public static final Logger LOGGER = LoggerFactory.getLogger(ServiceInstance.class);

    /**
     * 服务实例存活的阈值
     */
    public static final long ALIVE_TIME_THRESHOLD = 90 * 1000;

    private String serviceName = "inventory-service";
    private String ip = "192.168.31.207";
    private String hostname = "inventory01";
    private int port = 9000;
    private String instanceId;
    private Lease lease;

    public ServiceInstance() {
        this.lease = new Lease();
    }

    /**
     * 服务续约
     */
    public void renew() {
        this.lease.renew();
    }

    /**
     * 是否存活
     *
     * @return 是否存活
     */
    public boolean isAlive() {
        return this.lease.isAlive();
    }

    /**
     * 服务契约
     */
    private class Lease {

        private volatile Long lastHeartBeatTimestamp;

        public Long getLastHeartBeatTimestamp() {
            return lastHeartBeatTimestamp;
        }

        public Lease() {
            this.lastHeartBeatTimestamp = System.currentTimeMillis();
        }

        /**
         * 服务续约
         */
        public void renew() {
            this.lastHeartBeatTimestamp = System.currentTimeMillis();
            LOGGER.info("服务实例【" + instanceId + "】，进行续约：" + lastHeartBeatTimestamp);
        }

        /**
         * 是否存活
         *
         * @return 是否存活
         */
        public boolean isAlive() {
            if (System.currentTimeMillis() - this.getLastHeartBeatTimestamp() > ALIVE_TIME_THRESHOLD) {
                LOGGER.info("服务实例【" + instanceId + "】，不再存活");
                return false;
            }
            LOGGER.info("服务实例【" + instanceId + "】，保持存活");
            return true;
        }

        @Override
        public String toString() {
            return "Lease{" +
                    "lastHeartBeatTimestamp=" + lastHeartBeatTimestamp +
                    '}';
        }
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    @Override
    public String toString() {
        return "ServiceInstance{" +
                "serviceName='" + serviceName + '\'' +
                ", ip='" + ip + '\'' +
                ", hostname='" + hostname + '\'' +
                ", port=" + port +
                ", instanceId='" + instanceId + '\'' +
                ", lease=" + lease +
                '}';
    }
}
