package com.doodream.rmovjs.net;

import com.doodream.rmovjs.model.*;
import com.doodream.rmovjs.serde.Converter;
import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;


public class BaseServiceProxy implements RMIServiceProxy {

    private static final Logger Log = LogManager.getLogger(BaseServiceProxy.class);

    private volatile boolean isOpened;
    private RMIServiceInfo serviceInfo;
    private RMISocket socket;
    private Converter converter;
    private Reader reader;
    private Writer writer;


    public static BaseServiceProxy create(RMIServiceInfo info, RMISocket socket)  {
        return new BaseServiceProxy(info, socket);
    }

    private BaseServiceProxy(RMIServiceInfo info, RMISocket socket)  {
        serviceInfo = info;
        isOpened = false;
        this.socket = socket;
    }

    @Override
    public synchronized void open() throws IOException, IllegalAccessException, InstantiationException {
        if(isOpened) {
            return;
        }
        Log.debug("Try to connect {}", socket.getRemoteName());
        RMINegotiator negotiator = (RMINegotiator) serviceInfo.getNegotiator().newInstance();
        this.converter = (Converter) serviceInfo.getConverter().newInstance();
        socket.open();
        reader = converter.reader(socket.getInputStream());
        writer = converter.writer(socket.getOutputStream());
        socket = negotiator.handshake(socket, serviceInfo, converter, true);
        Log.info("proxy for {} opened", serviceInfo.getName());
        isOpened = true;
    }

    @Override
    public boolean isOpen() {
        return isOpened;
    }

    @Override
    public Response request(Endpoint endpoint, Object ...args) {

        return Observable.just(Request.builder())
                .map(builder -> builder.endpoint(endpoint.getUnique()))
                .map(builder -> builder.params(endpoint.convertParams(args)))
                .map(Request.RequestBuilder::build)
                .doOnNext(request -> Log.debug("request => {}", request))
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

    public void close() throws IOException {
        if(!isOpened) {
            return;
        }
        Log.debug("proxy for {} closed", serviceInfo.getName());
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
