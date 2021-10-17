package com.jonnyliu.proj.register.commons;

import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 代表一个服务实例
 *
 * @author liujie
 */
public class ServiceInstance {

    public static final Logger log = LoggerFactory.getLogger(ServiceInstance.class);

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
    }

    public Lease getLease() {
        return lease;
    }

    public void setLease(Lease lease) {
        this.lease = lease;
    }

    /**
     * 服务续约
     */
    public void renew() {
        if (this.lease == null) {
            this.lease = new Lease();
        }
        this.lease.renew();
    }

    /**
     * 是否存活
     *
     * @return 是否存活
     */
    public boolean isAlive() {
        if (this.lease == null) {
            this.lease = new Lease();
        }
        return this.lease.isAlive();
    }

    /**
     * 服务契约
     */
    private class Lease {

        /**
         * 最后一次的心跳时间戳
         */
        private volatile Long lastHeartBeatTimestamp;

        public Long getLastHeartBeatTimestamp() {
            return lastHeartBeatTimestamp;
        }

        public Lease() {
        }

        /**
         * 服务续约
         */
        public void renew() {
            this.lastHeartBeatTimestamp = System.currentTimeMillis();
            log.info("服务实例【" + instanceId + "】，进行续约：" + new Date(lastHeartBeatTimestamp));
        }

        /**
         * 是否存活
         *
         * @return 是否存活
         */
        public boolean isAlive() {
            if (System.currentTimeMillis() - this.getLastHeartBeatTimestamp() > ALIVE_TIME_THRESHOLD) {
                log.info("服务实例【{}】，不再存活", instanceId);
                return false;
            }
            log.info("服务实例【{}】，保持存活", instanceId);
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