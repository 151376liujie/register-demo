package com.jonnyliu.proj.register.commons;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author liujie
 */
public class Applications implements Serializable {

    private Map<String, Map<String, ServiceInstance>> registry = new ConcurrentHashMap<>();

    public Applications() {
    }

    public Applications(
            Map<String, Map<String, ServiceInstance>> registry) {
        this.registry = registry;
    }

    public Map<String, Map<String, ServiceInstance>> getRegistry() {
        return registry;
    }
}