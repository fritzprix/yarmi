package net.doodream.yarmi.test.net.noreply;

import net.doodream.yarmi.model.RMIServiceInfo;
import net.doodream.yarmi.net.ServiceProxy;
import net.doodream.yarmi.net.ServiceProxyFactory;
import net.doodream.yarmi.net.SimpleServiceProxy;

public class NoReplyServiceFactory implements ServiceProxyFactory {
    private RMIServiceInfo serviceInfo;

    @Override
    public ServiceProxy build() {
        return SimpleServiceProxy.create(serviceInfo, new NoReplyRMISocket());
    }

    @Override
    public void setTargetService(RMIServiceInfo info) {
        serviceInfo = info;
    }
}
