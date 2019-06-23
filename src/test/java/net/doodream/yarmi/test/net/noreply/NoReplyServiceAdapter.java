package net.doodream.yarmi.test.net.noreply;

import net.doodream.yarmi.data.RMIServiceInfo;
import net.doodream.yarmi.net.BaseServiceAdapter;
import net.doodream.yarmi.net.RMISocket;
import net.doodream.yarmi.net.ServiceProxyFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;

public class NoReplyServiceAdapter extends BaseServiceAdapter {
    private final ArrayBlockingQueue<RMISocket> clientQueue = new ArrayBlockingQueue<>(2);
    private volatile boolean isClosed;

    @Override
    protected void onStart(InetAddress bindAddress) throws IOException {
        isClosed = false;
        clientQueue.offer(new NoReplyRMISocket());
    }

    @Override
    protected boolean isClosed() {
        return isClosed;
    }

    @Override
    protected String getProxyConnectionHint(RMIServiceInfo serviceInfo) {
        return "";
    }

    @Override
    protected RMISocket acceptClient() throws IOException {
        try {
            return clientQueue.take();
        } catch (InterruptedException e) {
            throw new IOException(e);
        }
    }

    @Override
    protected void onClose() throws IOException {
        clientQueue.clear();
        isClosed = true;
    }

    @Override
    public ServiceProxyFactory getProxyFactory(RMIServiceInfo info) throws IllegalAccessException, InstantiationException {
        ServiceProxyFactory proxyFactory =  new NoReplyServiceFactory();
        proxyFactory.setTargetService(info);
        return proxyFactory;
    }

    @Override
    public void configure(Map<String, String> params) {

    }
}
