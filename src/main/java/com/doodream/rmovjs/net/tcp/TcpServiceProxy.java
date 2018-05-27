package com.doodream.rmovjs.net.tcp;

import com.doodream.rmovjs.model.*;
import com.doodream.rmovjs.net.RMINegotiator;
import com.doodream.rmovjs.net.RMIServiceProxy;
import com.doodream.rmovjs.net.RMISocket;
import com.doodream.rmovjs.serde.Converter;
import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;


public class TcpServiceProxy implements RMIServiceProxy {

    private static final Logger Log = LogManager.getLogger(TcpServiceProxy.class);

    private volatile boolean isOpened;
    private RMIServiceInfo serviceInfo;
    private RMISocket socket;
    private Converter converter;
    private Reader reader;
    private Writer writer;


    public static TcpServiceProxy create(RMIServiceInfo info, RMISocket socket)  {
        return new TcpServiceProxy(info, socket);
    }

    private TcpServiceProxy(RMIServiceInfo info, RMISocket socket)  {
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
        this.converter = (Converter) serviceInfo.getConverter().newInstance();
        socket.open();
        reader = converter.reader(socket.getInputStream());
        writer = converter.writer(socket.getOutputStream());
        socket = negotiator.handshake(socket, serviceInfo, converter, true);
        Log.info("Successfully Opened");
        isOpened = true;
    }

    @Override
    public boolean isOpen() {
        return isOpened;
    }

    @Override
    public Response request(Endpoint endpoint) {

        return Observable.just(Request.builder())
                .map(builder -> builder.endpoint(endpoint))
                .map(Request.RequestBuilder::build)
                .doOnNext(request -> converter.write(request, writer))
                .map(request -> converter.read(reader, Response.class))
                .doOnError(this::onError)
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
