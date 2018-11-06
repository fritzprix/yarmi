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
    private InetSocketAddress mAddress;
    public static final String DEFAULT_PORT = "6644";

    public TcpServiceAdapter(String host, String port) throws UnknownHostException {
        Log.debug("ServiceAdapter On {}/{}", host, port);
        int p = Integer.valueOf(port);
        mAddress = new InetSocketAddress(InetAddress.getByName(host), p);
    }

    public TcpServiceAdapter(String port) throws UnknownHostException {
        this(Inet4Address.getLocalHost().getHostAddress(), port);
    }

    public TcpServiceAdapter() throws UnknownHostException {
        this(DEFAULT_PORT);
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
    protected void onStart() throws IOException {
        serverSocket = new ServerSocket();
        serverSocket.bind(mAddress);
        Log.debug("service started @ {}", mAddress.getAddress().getHostAddress());
    }

    @Override
    protected void onClose() throws IOException {
        Log.debug("close() @ {}", mAddress.getAddress());
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
        return mAddress.getAddress().getHostAddress();
    }

    @Override
    protected RMISocket acceptClient() throws IOException {
        return new TcpRMISocket(serverSocket.accept());
    }

}
