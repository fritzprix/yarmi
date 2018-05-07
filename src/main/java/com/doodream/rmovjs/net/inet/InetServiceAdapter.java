package com.doodream.rmovjs.net.inet;


import com.doodream.rmovjs.model.Request;
import com.doodream.rmovjs.model.Response;
import com.doodream.rmovjs.model.ServiceInfo;
import com.doodream.rmovjs.net.ClientSocketAdapter;
import com.doodream.rmovjs.net.ServiceAdapter;
import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Function;

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
    private int port;

    public InetServiceAdapter(InetAddress address, int port) {
        this.port = port;
        mAddress = new InetSocketAddress(address, port);
        clientAdapterFactory = new InetClientSocketAdapterFactory();
        compositeDisposable = new CompositeDisposable();
    }

    public InetServiceAdapter(int port) throws UnknownHostException {
        this(InetAddress.getLocalHost(), port);
    }

    public InetServiceAdapter() throws UnknownHostException {
        this(3000);
    }

    @Override
    public void listen(ServiceInfo serviceInfo, Function<Request, Response> onRequest) throws IOException {
        assert onRequest != null;
        serverSocket = new ServerSocket();
        compositeDisposable.add(Observable.just(serverSocket)
                .doOnNext(socket -> socket.bind(mAddress))
                .map(socket -> clientAdapterFactory.handshake(serviceInfo, new InetRMISocket(serverSocket.accept())))
                .map(clientAdapterObservable -> clientAdapterObservable.doOnComplete(this::onComplete))
                .map(clientAdapterObservable -> clientAdapterObservable.doOnError(this::onError))
                .map(clientAdapterObservable -> clientAdapterObservable.subscribe(adapter -> subscribe(adapter, onRequest)))
                .subscribe(compositeDisposable::add));
    }

    private void onComplete() {
        System.out.println("RMIService Complete");
    }

    private void onError(Throwable throwable) {
        System.out.println(throwable);
    }

    private void subscribe(ClientSocketAdapter adapter, Function<Request, Response> requestHandler) throws IOException {
        compositeDisposable.add(adapter
                .listen()
                .map(requestHandler)
                .subscribe(adapter::write));
    }


    @Override
    public void close() throws IOException {
        if(serverSocket != null
                && !serverSocket.isClosed()) {
                serverSocket.close();
        }
    }

}
