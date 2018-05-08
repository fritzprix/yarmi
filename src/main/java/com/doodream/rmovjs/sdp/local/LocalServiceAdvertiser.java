package com.doodream.rmovjs.sdp.local;

import com.doodream.rmovjs.model.ServiceInfo;
import com.doodream.rmovjs.sdp.ServiceAdvertiser;
import lombok.Data;

import java.io.IOException;

/**
 *  this class is to be used in testing as a simple placeholder for service discovery
 *  feature within local machine.
 *
 *  advertising will be started by writing service registry into a key-value cache in local machine
 *
 */
@Data
public class LocalServiceAdvertiser implements ServiceAdvertiser {

    private ServiceInfo serviceInfo;

    @Override
    public void startAdvertiser(ServiceInfo info) throws IOException {
        serviceInfo = info;
        LocalServiceRegistry.registerService(serviceInfo);
    }

    @Override
    public void stopAdvertiser() throws IOException {
        if(serviceInfo == null) {
            return;
        }
        LocalServiceRegistry.unregisterService(serviceInfo);
    }
}
