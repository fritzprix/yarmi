package com.doodream.rmovjs.net.inet;

import com.doodream.rmovjs.model.RMIServiceInfo;
import com.doodream.rmovjs.net.RMIServiceProxy;
import com.doodream.rmovjs.net.ServiceProxyFactory;

import java.io.IOException;

public class InetServiceProxyFactory implements ServiceProxyFactory {
    private int port;
    private String host;

    public InetServiceProxyFactory(String address, String port) {
        host = address;
        this.port = Integer.valueOf(port);
    }

    public InetServiceProxyFactory(String port) {
        this(InetServiceAdapter.DEFAULT_NAME, port);
    }

    public InetServiceProxyFactory() {
        this(InetServiceAdapter.DEFAULT_PORT);
    }

    @Override
    public RMIServiceProxy build(RMIServiceInfo info) throws IOException {
        return InetServiceProxy.create(info, new InetRMISocket(host, port));
    }
}
