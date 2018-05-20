package com.doodream.rmovjs.net.inet;


import com.doodream.rmovjs.model.RMIServiceInfo;
import com.doodream.rmovjs.model.Request;
import com.doodream.rmovjs.model.Response;
import com.doodream.rmovjs.net.ClientSocketAdapter;
import com.doodream.rmovjs.net.ServiceAdapter;
import com.doodream.rmovjs.net.ServiceProxyFactory;
import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Function;
import lombok.NonNull;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;

public class InetServiceAdapter implements ServiceAdapter {

    private ServerSocket serverSocket;
    private InetSocketAddress mAddress;
    private InetClientSocketAdapterFactory clientAdapterFactory;
    private CompositeDisposable compositeDisposable;
    private volatile boolean listen;
    public static final String DEFAULT_NAME = "localhost";
    public static final String DEFAULT_PORT = "6644";

    public InetServiceAdapter(String host, String port) throws UnknownHostException {
        int p = Integer.valueOf(port);
        mAddress = new InetSocketAddress(InetAddress.getByName(host), p);
        clientAdapterFactory = new InetClientSocketAdapterFactory();
        compositeDisposable = new CompositeDisposable();
    }

    public InetServiceAdapter(String port) throws UnknownHostException {
        this(DEFAULT_NAME, port);
    }

    public InetServiceAdapter() throws UnknownHostException {
        this(DEFAULT_PORT);
    }

    @Override
    public void listen(RMIServiceInfo serviceInfo, @NonNull Function<Request, Response> handleRequest) throws IOException {
        serverSocket = new ServerSocket();
        serverSocket.bind(mAddress);
        compositeDisposable.add(Observable.just(serverSocket)
                .map(ServerSocket::accept)
                .repeatUntil(() -> !listen)
                .map(socket -> clientAdapterFactory.handshake(serviceInfo, new InetRMISocket(socket)))
                .doOnNext(adapter-> onHandshakeSuccess(adapter, handleRequest))
                .subscribe(clientSocketAdapter -> {},this::onError));

    }



    private void onHandshakeSuccess(ClientSocketAdapter adapter, Function<Request, Response> handleRequest) throws IOException {
        compositeDisposable.add(adapter
                .listen()
                .doOnNext(request -> request.setClient(adapter))
                .doOnNext(request -> System.out.println("Server <= " + request))
                .filter(Request::valid)
                .map(handleRequest)
                .doOnError(this::onError)
                .doOnNext(adapter::write)
                .subscribe());
    }


    @Override
    public ServiceProxyFactory getProxyFactory(RMIServiceInfo info) {
        // RMIServiceInfo
        String[] params = info.getParams().toArray(new String[0]);
        return Observable.fromArray(InetServiceProxyFactory.class.getConstructors())
                .filter(constructor -> constructor.getParameterCount() == params.length)
                .map(constructor -> constructor.newInstance(params))
                .cast(ServiceProxyFactory.class)
                .blockingFirst();
    }


    private void onComplete() {
        System.out.println("RMIService Complete");
    }

    private void onError(Throwable throwable) throws IOException {
        if(serverSocket.isClosed()) {
            return;
        }
        serverSocket.close();
    }

    private void onDispose() throws IOException {
        close();
    }


    @Override
    public void close() throws IOException {
        System.out.println("Closed");
        if(serverSocket != null
                && !serverSocket.isClosed()) {
                serverSocket.close();
        }
        compositeDisposable.dispose();
        compositeDisposable.clear();
    }

}
