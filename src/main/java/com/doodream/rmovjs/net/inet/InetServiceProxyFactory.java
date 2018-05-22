package com.doodream.rmovjs.net.inet;

import com.doodream.rmovjs.model.RMIServiceInfo;
import com.doodream.rmovjs.net.RMIServiceProxy;
import com.doodream.rmovjs.net.ServiceProxyFactory;

import java.io.IOException;

public class InetServiceProxyFactory implements ServiceProxyFactory {
    private RMIServiceInfo serviceInfo;
    private int port;
    private String host;

    public InetServiceProxyFactory(String address, String port) {
        host = address;
        this.port = Integer.valueOf(port);
    }

    public InetServiceProxyFactory(String port) {
        this(null, port);
    }

    public InetServiceProxyFactory() {
        this(InetServiceAdapter.DEFAULT_PORT);
    }

    @Override
    public RMIServiceProxy build() throws IOException {
        if(host == null) {
            host = serviceInfo.getProxyFactoryHint();
        }
        return InetServiceProxy.create(serviceInfo, new InetRMISocket(host, port));
    }

    @Override
    public void setTargetService(RMIServiceInfo info) {
        serviceInfo = info;
    }
}
