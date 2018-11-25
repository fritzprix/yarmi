package com.doodream.rmovjs.sdp;

import java.io.IOException;
import java.net.InetAddress;
import java.util.concurrent.TimeUnit;

public interface ServiceDiscovery {
    void startDiscovery(Class service , boolean once, long timeout, TimeUnit unit, InetAddress network, ServiceDiscoveryListener listener) throws IOException, IllegalAccessException, InstantiationException;
    void startDiscovery(Class service, boolean once, InetAddress network, ServiceDiscoveryListener listener) throws IOException, InstantiationException, IllegalAccessException;
    void startDiscovery(Class service, boolean once, long timeout, TimeUnit unit, ServiceDiscoveryListener listener) throws IOException, IllegalAccessException, InstantiationException;
    void startDiscovery(Class service, boolean once, ServiceDiscoveryListener listener) throws IOException, IllegalAccessException, InstantiationException;
    void cancelDiscovery(Class service) throws IOException;
}
