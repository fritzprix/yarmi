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
import io.reactivex.schedulers.Schedulers;
import lombok.NonNull;

import java.io.IOException;
import java.net.*;

public class InetServiceAdapter implements ServiceAdapter {

    private ServerSocket serverSocket;
    private InetSocketAddress mAddress;
    private InetClientSocketAdapterFactory clientAdapterFactory;
    private CompositeDisposable compositeDisposable;
    private volatile boolean listen = false;
    public static final String DEFAULT_PORT = "6644";

    public InetServiceAdapter(String host, String port) throws UnknownHostException {
        int p = Integer.valueOf(port);
        mAddress = new InetSocketAddress(InetAddress.getByName(host), p);
        clientAdapterFactory = new InetClientSocketAdapterFactory();
        compositeDisposable = new CompositeDisposable();
    }

    public InetServiceAdapter(String port) throws UnknownHostException {
        this(Inet4Address.getLocalHost().getHostAddress(), port);
    }

    public InetServiceAdapter() throws UnknownHostException {
        this(DEFAULT_PORT);
    }

    @Override
    public String listen(RMIServiceInfo serviceInfo, @NonNull Function<Request, Response> handleRequest) throws IOException {
        serverSocket = new ServerSocket();
        listen = true;
        serverSocket.bind(mAddress);
        compositeDisposable.add(Observable.just(serverSocket)
                .map(ServerSocket::accept)
                .doOnNext(client -> System.out.println(client.getInetAddress()))
                .repeatUntil(() -> !listen)
                .map(socket -> clientAdapterFactory.handshake(serviceInfo, new InetRMISocket(socket)))
                .subscribeOn(Schedulers.io())
                .subscribe(adapter-> onHandshakeSuccess(adapter, handleRequest),this::onError));

        return mAddress.getAddress().getHostAddress();
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
        if(!RMIServiceInfo.isComplete(info)) {
            throw new IllegalArgumentException("Incomplete service info");
        }
        String[] params = info.getParams().toArray(new String[0]);
        return Observable.fromArray(InetServiceProxyFactory.class.getConstructors())
                .filter(constructor -> constructor.getParameterCount() == params.length)
                .map(constructor -> constructor.newInstance(params))
                .cast(ServiceProxyFactory.class)
                .doOnNext(serviceProxyFactory -> serviceProxyFactory.setTargetService(info))
                .blockingFirst();
    }


    private void onError(Throwable throwable) throws IOException {
        if(serverSocket.isClosed()) {
            return;
        }
        serverSocket.close();
    }


    @Override
    public void close() throws IOException {
        System.out.println("Closed");
        listen = false;
        if(serverSocket != null
                && !serverSocket.isClosed()) {
                serverSocket.close();
        }
        compositeDisposable.dispose();
        compositeDisposable.clear();
    }

}
