package com.doodream.rmovjs.sdp;

import com.doodream.rmovjs.model.ServiceInfo;
import com.doodream.rmovjs.net.RMIServiceProxy;
import io.reactivex.Observable;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public interface RMIServiceDiscovery {
    Observable<RMIServiceProxy> discover(ServiceInfo info, long timeout, TimeUnit unit) throws IOException;
    Observable<RMIServiceProxy> startDiscovery(ServiceInfo info) throws IOException;
    void stopDiscovery() throws IOException;
}
