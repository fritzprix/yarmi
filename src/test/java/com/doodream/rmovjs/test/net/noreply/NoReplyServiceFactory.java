package com.doodream.rmovjs.test.net.noreply;

import com.doodream.rmovjs.model.RMIServiceInfo;
import com.doodream.rmovjs.net.ServiceProxy;
import com.doodream.rmovjs.net.ServiceProxyFactory;
import com.doodream.rmovjs.net.SimpleServiceProxy;

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
