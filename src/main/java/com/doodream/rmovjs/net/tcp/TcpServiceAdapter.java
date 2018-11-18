package com.doodream.rmovjs.net.tcp;


import com.doodream.rmovjs.model.RMIServiceInfo;
import com.doodream.rmovjs.net.BaseServiceAdapter;
import com.doodream.rmovjs.net.RMISocket;
import com.doodream.rmovjs.net.ServiceProxyFactory;
import io.reactivex.Observable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.*;

public class TcpServiceAdapter extends BaseServiceAdapter {

    protected static final Logger Log = LoggerFactory.getLogger(TcpServiceAdapter.class);

    private ServerSocket serverSocket;
    private int port = DEFAULT_PORT;
    public static final int DEFAULT_PORT = 6644;

    public TcpServiceAdapter(String port) {
        this.port = Integer.valueOf(port);
    }

    @Override
    public ServiceProxyFactory getProxyFactory(final RMIServiceInfo info) {
        if(!RMIServiceInfo.isValid(info)) {
            throw new IllegalArgumentException("Incomplete service info");
        }
        final String[] params = info.getParams().toArray(new String[0]);
        return Observable.fromArray(TcpServiceProxyFactory.class.getConstructors())
                .filter(new Predicate<Constructor<?>>() {
                    @Override
                    public boolean test(Constructor<?> constructor) throws Exception {
                        return constructor.getParameterCount() == params.length;
                    }
                })
                .map(new Function<Constructor<?>, Object>() {
                    @Override
                    public Object apply(Constructor<?> constructor) throws Exception {
                        return constructor.newInstance(params);
                    }
                })
                .cast(ServiceProxyFactory.class)
                .doOnNext(new Consumer<ServiceProxyFactory>() {
                    @Override
                    public void accept(ServiceProxyFactory serviceProxyFactory) throws Exception {
                        serviceProxyFactory.setTargetService(info);
                    }
                })
                .blockingFirst();
    }

    @Override
    protected void onStart(InetAddress bindAddress) throws IOException {
        serverSocket = new ServerSocket();
        serverSocket.bind(new InetSocketAddress(bindAddress.getHostAddress(), port));
        Log.debug("service started @ {}", bindAddress.getHostAddress());
    }

    @Override
    protected void onClose() throws IOException {
        Log.debug("close() @ {}", serverSocket.getInetAddress().getAddress());
        if(serverSocket != null
                && !serverSocket.isClosed()) {
            serverSocket.close();
        }
    }

    @Override
    protected boolean isClosed() {
        return serverSocket.isClosed();
    }

    @Override
    protected String getProxyConnectionHint(RMIServiceInfo serviceInfo) {
        return serverSocket.getInetAddress().getHostAddress();
    }

    @Override
    protected RMISocket acceptClient() throws IOException {
        return new TcpRMISocket(serverSocket.accept());
    }

}
