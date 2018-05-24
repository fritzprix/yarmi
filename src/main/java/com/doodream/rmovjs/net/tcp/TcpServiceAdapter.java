package com.doodream.rmovjs.net.tcp;


import com.doodream.rmovjs.model.RMIServiceInfo;
import com.doodream.rmovjs.model.Request;
import com.doodream.rmovjs.model.Response;
import com.doodream.rmovjs.net.ClientSocketAdapter;
import com.doodream.rmovjs.net.RMINegotiator;
import com.doodream.rmovjs.net.ServiceAdapter;
import com.doodream.rmovjs.net.ServiceProxyFactory;
import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import lombok.NonNull;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.*;

public class TcpServiceAdapter implements ServiceAdapter {

    private static final Logger Log = LogManager.getLogger(TcpServiceAdapter.class);

    private ServerSocket serverSocket;
    private InetSocketAddress mAddress;
    private CompositeDisposable compositeDisposable;
    private volatile boolean listen = false;
    public static final String DEFAULT_PORT = "6644";

    public TcpServiceAdapter(String host, String port) throws UnknownHostException {
        int p = Integer.valueOf(port);
        mAddress = new InetSocketAddress(InetAddress.getByName(host), p);
        compositeDisposable = new CompositeDisposable();
    }

    public TcpServiceAdapter(String port) throws UnknownHostException {
        this(Inet4Address.getLocalHost().getHostAddress(), port);
    }

    public TcpServiceAdapter() throws UnknownHostException {
        this(DEFAULT_PORT);
    }

    @Override
    public String listen(RMIServiceInfo serviceInfo, @NonNull Function<Request, Response> handleRequest) throws IOException, IllegalAccessException, InstantiationException {
        Log.debug("Service Listen @ {} : {}", mAddress.getAddress(), mAddress.getPort());
        serverSocket = new ServerSocket();
        serverSocket.bind(mAddress);

        RMINegotiator negotiator = (RMINegotiator) serviceInfo.getNegotiator().newInstance();
        listen = true;

        compositeDisposable.add(Observable.just(serverSocket)
                .map(ServerSocket::accept)
                .doOnNext(client -> Log.debug("Client @ {}", client.getInetAddress()))
                .repeatUntil(() -> !listen)
                .map(TcpRMISocket::new)
                .map(client -> negotiator.handshake(client, serviceInfo, false))
                .map(TcpClientSocketAdapter::new)
                .subscribeOn(Schedulers.io())
                .subscribe(adapter-> onHandshakeSuccess(adapter, handleRequest),this::onError));

        return mAddress.getAddress().getHostAddress();
    }



    private void onHandshakeSuccess(ClientSocketAdapter adapter, Function<Request, Response> handleRequest) throws IOException {
        compositeDisposable.add(adapter
                .listen()
                .doOnNext(request -> request.setClient(adapter))
                .doOnNext(request -> Log.info("Server <= {}", request))
                .filter(Request::isValid)
                .map(handleRequest)
                .doOnError(this::onError)
                .doOnNext(adapter::write)
                .subscribe());
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


    private void onError(Throwable throwable) throws IOException {
        Log.error(throwable);
        if(serverSocket.isClosed()) {
            return;
        }
        serverSocket.close();
    }


    @Override
    public void close() throws IOException {
        Log.debug("close() @ {}", mAddress.getAddress());
        listen = false;
        if(serverSocket != null
                && !serverSocket.isClosed()) {
                serverSocket.close();
        }
        compositeDisposable.dispose();
        compositeDisposable.clear();
    }

}
