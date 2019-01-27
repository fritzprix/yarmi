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
import java.util.Map;

public class TcpServiceAdapter extends BaseServiceAdapter {

    public static final String PARAM_HOST = "tcp.host";
    public static final String PARAM_PORT = "tcp.port";
    protected static final Logger Log = LoggerFactory.getLogger(TcpServiceAdapter.class);

    private ServerSocket serverSocket;
    private int port;
    static final int DEFAULT_PORT = 6644;

    public TcpServiceAdapter() {
        port = DEFAULT_PORT;
    }

    @Override
    public ServiceProxyFactory getProxyFactory(final RMIServiceInfo info) throws IllegalAccessException, InstantiationException {
        if(!RMIServiceInfo.isValid(info)) {
            throw new IllegalArgumentException("Incomplete service info");
        }


        TcpServiceProxyFactory factory = TcpServiceProxyFactory.class.newInstance();
        factory.setTargetService(info);
        return factory;
    }

    @Override
    public void configure(Map<String, String> params) {
        final String portStr =  params.get(PARAM_PORT);
        if(portStr == null) {
            port = DEFAULT_PORT;
        } else {
            port = Integer.valueOf(portStr);
        }
    }


    @Override
    protected void onStart(InetAddress bindAddress) throws IOException {
        serverSocket = new ServerSocket();
        serverSocket.bind(new InetSocketAddress(bindAddress.getHostAddress(), port));
        Log.debug("service started @ {} : {}", serverSocket.getLocalSocketAddress(), serverSocket.getInetAddress().getHostAddress());
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
