package com.doodream.rmovjs.net.inet;


import com.doodream.rmovjs.model.RMIError;
import com.doodream.rmovjs.model.RMIServiceInfo;
import com.doodream.rmovjs.model.Request;
import com.doodream.rmovjs.model.Response;
import com.doodream.rmovjs.net.ClientSocketAdapter;
import com.doodream.rmovjs.net.RMISocket;
import com.doodream.rmovjs.net.SerdeUtil;
import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class InetClientSocketAdapter implements ClientSocketAdapter {


    private RMIServiceInfo serviceInfo;
    private RMISocket client;
    private Observable<String> lineObservable;
    private BufferedReader reader;


    InetClientSocketAdapter(RMISocket socket) throws IOException {
        client = socket;
        reader = new BufferedReader(new InputStreamReader(client.getInputStream()));
    }


    @Override
    public boolean handshake(RMIServiceInfo serviceInfo) throws IOException {
        this.serviceInfo = serviceInfo;

        Observable<RMIServiceInfo> handshakeRequestSingle = Observable.just(reader.readLine())
                .map(s -> SerdeUtil.fromJson(s, RMIServiceInfo.class));

            Observable<Response> serviceInfoMatchedObservable = handshakeRequestSingle
                    .filter(info -> info.hashCode() == serviceInfo.hashCode())
                    .map(info -> Response.success("OK"));

            Observable<Response> serviceInfoMismatchObservable = handshakeRequestSingle
                    .filter(info -> info.hashCode() != serviceInfo.hashCode())
                    .map(info -> RMIError.FORBIDDEN.getResponse(Request.builder().build()));

            return serviceInfoMatchedObservable.mergeWith(serviceInfoMismatchObservable)
                    .doOnNext(this::write)
                    .map(Response::isSuccessful)
                    .doOnNext(this::setListenable)
                    .first(false).blockingGet();
    }

    /**
     *
     * @param success true if handshake is successful, otherwise, false.
     */
    private void setListenable(Boolean success) {
        if(success) {
            lineObservable = Observable.create(emitter -> {
                try {
                    String line;
                    while((line = reader.readLine()) != null) {
                        emitter.onNext(line);
                    }
                    emitter.onComplete();
                } catch (IOException e) {
                    emitter.onError(e);
                }
            });
        }
    }


    @Override
    public void write(Response response) throws IOException {
        client.getOutputStream().write(SerdeUtil.toByteArray(response));
    }

    @Override
    public Observable<Request> listen() throws IOException {
        return lineObservable.subscribeOn(Schedulers.io()).map(Request::fromJson);
    }

    @Override
    public String who() {
        return client.getRemoteName();
    }

}
