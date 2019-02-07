package com.doodream.rmovjs.net.tcp;

import com.doodream.rmovjs.model.RMIServiceInfo;
import com.doodream.rmovjs.net.SimpleServiceProxy;
import com.doodream.rmovjs.net.ServiceProxy;
import com.doodream.rmovjs.net.ServiceProxyFactory;

import java.util.Map;

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
    public ServiceProxy build() {
        if(host == null) {
            host = serviceInfo.getProxyFactoryHint();
        }
        return SimpleServiceProxy.create(serviceInfo, new TcpRMISocket(host, port));
    }

    @Override
    public void setTargetService(RMIServiceInfo info) {
        serviceInfo = info;
        Map<String, String> params = info.getParams();
        host = params.get(TcpServiceAdapter.PARAM_HOST);
        final String portParam = params.get(TcpServiceAdapter.PARAM_PORT);
        if(portParam == null) {
            port = TcpServiceAdapter.DEFAULT_PORT;
        } else {
            port = Integer.valueOf(portParam);
        }
    }
}
