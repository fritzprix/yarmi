package com.doodream.rmovjs.net.tcp;


import com.doodream.rmovjs.model.RMIServiceInfo;
import com.doodream.rmovjs.net.*;
import io.reactivex.Observable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.*;

public class TcpServiceAdapter extends BaseServiceAdapter {

    protected static final Logger Log = LogManager.getLogger(TcpServiceAdapter.class);

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
    public ServiceProxyFactory getProxyFactory(RMIServiceInfo info) {
        if(!RMIServiceInfo.isComplete(info)) {
            throw new IllegalArgumentException("Incomplete service info");
        }
        String[] params = info.getParams().toArray(new String[0]);
        return Observable.fromArray(TcpServiceProxyFactory.class.getConstructors())
                .filter(constructor -> constructor.getParameterCount() == params.length)
                .map(constructor -> constructor.newInstance(params))
                .cast(ServiceProxyFactory.class)
                .doOnNext(serviceProxyFactory -> serviceProxyFactory.setTargetService(info))
                .blockingFirst();
    }

    @Override
    protected void onStart() throws IOException {
        serverSocket = new ServerSocket();
        Log.debug("service address {}", mAddress.getAddress().getHostAddress());
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
