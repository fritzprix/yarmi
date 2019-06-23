package net.doodream.yarmi.test.net.noreply;

import net.doodream.yarmi.data.RMIServiceInfo;
import net.doodream.yarmi.net.ServiceProxy;
import net.doodream.yarmi.net.ServiceProxyFactory;

public class NoReplyServiceFactory implements ServiceProxyFactory {
    private RMIServiceInfo serviceInfo;

    @Override
    public ServiceProxy build() {
        return ServiceProxy.getDefault(serviceInfo, new NoReplyRMISocket());
    }

    @Override
    public void setTargetService(RMIServiceInfo info) {
        serviceInfo = info;
    }
}
