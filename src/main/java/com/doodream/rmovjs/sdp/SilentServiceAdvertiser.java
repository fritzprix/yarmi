package com.doodream.rmovjs.sdp;

import com.doodream.rmovjs.model.RMIServiceInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;

public class SilentServiceAdvertiser implements ServiceAdvertiser{

    private static final Logger Log = LoggerFactory.getLogger(SilentServiceAdvertiser.class);
    private RMIServiceInfo serviceInfo;

    @Override
    public void startAdvertiser(RMIServiceInfo info, boolean block) throws IOException {
        serviceInfo = info;
        waitIndeterminately();
        if(block) {

        }
    }

    private synchronized void waitIndeterminately() {
        try {
            wait();
        } catch (InterruptedException e) {
            Log.warn("interrupted from waiting!");
        }
    }

    public RMIServiceInfo getServiceInfo() {
        return serviceInfo;
    }

    @Override
    public void startAdvertiser(RMIServiceInfo info, boolean block, InetAddress inf) throws IOException {
        serviceInfo = info;
        if(block) {
            waitIndeterminately();
        }
    }

    @Override
    public void stopAdvertiser() throws IOException {
        synchronized (this) {
            notifyAll();
        }
    }
}
