package com.doodream.rmovjs.net.inet;

import com.doodream.rmovjs.model.*;
import com.doodream.rmovjs.net.RMINegotiator;
import com.doodream.rmovjs.net.RMIServiceProxy;
import com.doodream.rmovjs.net.RMISocket;
import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import lombok.Data;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;


@Data
public class InetServiceProxy implements RMIServiceProxy {

    private static final Logger Log = LogManager.getLogger(InetServiceProxy.class);

    private RMIServiceInfo serviceInfo;
    private RMISocket socket;
    private BufferedReader reader;
    private PrintStream writer;


    public static InetServiceProxy create(RMIServiceInfo info, RMISocket socket) throws IOException {
        return new InetServiceProxy(info, socket);
    }

    private InetServiceProxy(RMIServiceInfo info, RMISocket socket) throws IOException {
        serviceInfo = info;
        this.socket = socket;
    }

    @Override
    public void open() throws IOException, IllegalAccessException, InstantiationException {
        socket.open();
        RMINegotiator negotiator = (RMINegotiator) serviceInfo.getNegotiator().newInstance();
        socket = negotiator.handshake(socket, serviceInfo, true);
        Log.info("Successfully Opened");
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    private void write(String s) throws IOException {
        socket.getOutputStream().write(s.concat("\n").getBytes());
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
        Log.debug("Closed");
        if(socket.isClosed()) {
            return;
        }
        socket.close();
    }

    @Override
    public boolean provide(Class controller) {
        return Observable.fromIterable(serviceInfo.getControllerInfos())
                .map(ControllerInfo::getStubCls)
                .map(controller::equals)
                .blockingFirst(false);
    }
}
