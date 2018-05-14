package com.doodream.rmovjs.net.inet;


import com.doodream.rmovjs.model.Request;
import com.doodream.rmovjs.model.Response;
import com.doodream.rmovjs.model.RMIServiceInfo;
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
import java.util.List;

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
        compositeDisposable.add(Observable
                .just(serverSocket)
                .doOnNext(socket -> socket.bind(mAddress))
                .to(this::onBindSuccess)
                .map(socket -> clientAdapterFactory.handshake(serviceInfo, new InetRMISocket(socket)))
                .map(clientAdapterObservable -> clientAdapterObservable.doOnComplete(this::onComplete))
                .map(clientAdapterObservable -> clientAdapterObservable.doOnError(this::onError))
                .map(clientAdapterObservable -> clientAdapterObservable.subscribe(adapter -> subscribe(adapter, handleRequest)))
                .subscribe(compositeDisposable::add));
    }

    @Override
    public ServiceProxyFactory getProxyFactory(RMIServiceInfo info) {
        // TODO: InetServiceProxyFactory 리턴
        List<String> params = info.getParams();
        // TODO: params는 InetServiceAdapter의 생성자에 해당하며 hostname의 현재 Default값인 Localhost의 경
        // remote의 client socket을 생성하는데 도움을 줄 수 없다.
        final int size = params.size();
        if(size > 1) {
            return new InetServiceProxyFactory(params.get(0), params.get(1));
        } else if(size > 0) {
            return new InetServiceProxyFactory(params.get(0));
        } else {
            return new InetServiceProxyFactory();
        }
    }

    private Observable<Socket> onBindSuccess(Observable<ServerSocket> serverSocketObservable) {
        return Observable.create(emitter -> {
            listen = true;
            compositeDisposable.add(serverSocketObservable.subscribe(serverSocket -> {
                try {
                    while (listen) {
                        emitter.onNext(serverSocket.accept());
                    }
                    emitter.onComplete();
                } catch (Exception e) {
                    emitter.onError(e);
                }
            }, this::onError));
        });
    }


    private void onComplete() {
        System.out.println("RMIService Complete");
    }

    private void onError(Throwable throwable) {
        throwable.printStackTrace();
    }

    private void subscribe(ClientSocketAdapter adapter, Function<Request, Response> requestHandler) throws IOException {
        compositeDisposable.add(adapter
                .listen()
                .map(requestHandler)
                .subscribe(adapter::write));
    }


    @Override
    public void close() throws IOException {
        System.out.println("Closed");
        if(serverSocket != null
                && !serverSocket.isClosed()) {
                serverSocket.close();
        }
    }

}
