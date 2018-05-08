package com.doodream.rmovjs.sdp.local;

import com.doodream.rmovjs.model.ServiceInfo;
import com.google.common.collect.Lists;

import java.util.HashSet;
import java.util.List;

public class LocalServiceRegistry {

    public static HashSet<ServiceInfo> SERVICE_REGISTRY = new HashSet<>();

    public static void registerService(ServiceInfo info) {
        SERVICE_REGISTRY.add(info);
    }

    public static void unregisterService(ServiceInfo info) {
        SERVICE_REGISTRY.remove(info);
    }

    public static List<ServiceInfo> listService() {
        return Lists.newArrayList(SERVICE_REGISTRY.iterator());
    }
}
