package com.doodream.rmovjs.net.inet;

import com.doodream.rmovjs.model.*;
import com.doodream.rmovjs.net.HandshakeFailException;
import com.doodream.rmovjs.net.RMIServiceProxy;
import com.doodream.rmovjs.net.RMISocket;
import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import lombok.Data;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;


@Data
public class InetServiceProxy implements RMIServiceProxy {

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
    public void open() throws IOException {
        socket.open();
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        write(this.serviceInfo.toJson());
        Response response = Response.fromJson(reader.readLine());
        if((response != null) &&
                response.isSuccessful()) {
            return;
        }
        throw new HandshakeFailException();
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
                .doOnError(throwable -> this.close())
                .map(s -> Response.fromJson(s, endpoint.getResponseType()))
                .subscribeOn(Schedulers.io())
                .blockingSingle();

    }


    @Override
    public void close() throws IOException {
        System.out.println("Closed");
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
