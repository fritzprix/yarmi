package com.doodream.rmovjs.sdp;

import com.doodream.rmovjs.model.RMIServiceInfo;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public interface ServiceDiscovery {
    void startDiscovery(Class service , ServiceDiscoveryListener listener, long timeout, TimeUnit unit) throws IOException;
    void startDiscovery(Class service, ServiceDiscoveryListener listener) throws IOException;
    void cancelDiscovery(Class service) throws IOException;
}
