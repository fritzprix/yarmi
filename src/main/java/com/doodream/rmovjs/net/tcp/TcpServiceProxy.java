package com.doodream.rmovjs.net.tcp;

import com.doodream.rmovjs.model.*;
import com.doodream.rmovjs.net.RMINegotiator;
import com.doodream.rmovjs.net.RMIServiceProxy;
import com.doodream.rmovjs.net.RMISocket;
import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;


public class TcpServiceProxy implements RMIServiceProxy {

    private static final Logger Log = LogManager.getLogger(TcpServiceProxy.class);

    private volatile boolean isOpened;
    private RMIServiceInfo serviceInfo;
    private RMISocket socket;
    private BufferedReader reader;
    private PrintStream writer;


    public static TcpServiceProxy create(RMIServiceInfo info, RMISocket socket) throws IOException {
        return new TcpServiceProxy(info, socket);
    }

    private TcpServiceProxy(RMIServiceInfo info, RMISocket socket) throws IOException {
        serviceInfo = info;
        isOpened = false;
        this.socket = socket;
    }

    @Override
    public synchronized void open() throws IOException, IllegalAccessException, InstantiationException {
        if(isOpened) {
            return;
        }
        RMINegotiator negotiator = (RMINegotiator) serviceInfo.getNegotiator().newInstance();
        socket.open();
        socket = negotiator.handshake(socket, serviceInfo, true);
        Log.info("Successfully Opened");
        isOpened = true;
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        writer = new PrintStream(socket.getOutputStream());
    }

    @Override
    public boolean isOpen() {
        return isOpened;
    }

    private void write(String s) throws IOException {
        writer.println(s);
        writer.flush();
    }

    @Override
    public Response request(Endpoint endpoint) {
        Request request = Request.builder()
                        .endpoint(endpoint)
                        .build();

        return Observable.just(request)
                .map(Request::toJson)
                .doOnNext(this::write)
                .map(s -> reader.readLine())
                .doOnError(this::onError)
                .map(s -> Response.fromJson(s, endpoint.getResponseType()))
                .subscribeOn(Schedulers.io())
                .blockingSingle();

    }

    private void onError(Throwable throwable) {
        Log.error(throwable);
        try {
            close();
        } catch (IOException ignored) { }
    }

    @Override
    public void close() throws IOException {
        if(!isOpened) {
            return;
        }
        Log.debug("Close()");
        if(socket.isClosed()) {
            return;
        }
        socket.close();
        isOpened = false;
    }

    @Override
    public boolean provide(Class controller) {
        return Observable.fromIterable(serviceInfo.getControllerInfos())
                .map(ControllerInfo::getStubCls)
                .map(controller::equals)
                .blockingFirst(false);
    }
}
