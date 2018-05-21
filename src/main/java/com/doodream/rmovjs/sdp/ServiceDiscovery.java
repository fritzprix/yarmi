package com.doodream.rmovjs.sdp;

import com.doodream.rmovjs.model.RMIServiceInfo;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public interface ServiceDiscovery {
    void startDiscovery(RMIServiceInfo info , ServiceDiscoveryListener listener, long timeout, TimeUnit unit) throws IOException;
    void startDiscovery(RMIServiceInfo info, ServiceDiscoveryListener listener) throws IOException;
    void cancelDiscovery(RMIServiceInfo info) throws IOException;
}
