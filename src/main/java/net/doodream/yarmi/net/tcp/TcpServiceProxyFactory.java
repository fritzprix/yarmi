package net.doodream.yarmi.net.tcp;

import net.doodream.yarmi.model.RMIServiceInfo;
import net.doodream.yarmi.net.ServiceProxy;
import net.doodream.yarmi.net.ServiceProxyFactory;

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
        return ServiceProxy.getDefault(serviceInfo, new TcpRMISocket(host, port));
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
