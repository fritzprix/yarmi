package com.doodream.rmovjs.sdp;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public interface ServiceDiscovery {
    void startDiscovery(Class service , ServiceDiscoveryListener listener, long timeout, TimeUnit unit) throws IOException, IllegalAccessException, InstantiationException;
    void startDiscovery(Class service, ServiceDiscoveryListener listener) throws IOException, InstantiationException, IllegalAccessException;
    void cancelDiscovery(Class service) throws IOException;
}
