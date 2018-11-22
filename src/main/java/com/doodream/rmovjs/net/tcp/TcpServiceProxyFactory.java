package com.doodream.rmovjs.net.tcp;

import com.doodream.rmovjs.model.RMIServiceInfo;
import com.doodream.rmovjs.net.BaseServiceProxy;
import com.doodream.rmovjs.net.RMIServiceProxy;
import com.doodream.rmovjs.net.ServiceProxyFactory;

public class TcpServiceProxyFactory implements ServiceProxyFactory {
    private RMIServiceInfo serviceInfo;
    private int port;
    private String host;

    public TcpServiceProxyFactory(String address, String port) {
        host = address;
        this.port = Integer.valueOf(port);
    }

    public TcpServiceProxyFactory(String port) {
        this(null, port);
    }

    public TcpServiceProxyFactory() {
        this(String.valueOf(TcpServiceAdapter.DEFAULT_PORT));
    }

    @Override
    public RMIServiceProxy build() {
        if(host == null) {
            host = serviceInfo.getProxyFactoryHint();
        }
        return BaseServiceProxy.create(serviceInfo, new TcpRMISocket(host, port));
    }

    @Override
    public void setTargetService(RMIServiceInfo info) {
        serviceInfo = info;
    }
}
