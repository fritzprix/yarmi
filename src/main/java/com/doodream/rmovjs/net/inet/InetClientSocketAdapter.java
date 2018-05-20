package com.doodream.rmovjs.net.inet;


import com.doodream.rmovjs.model.RMIError;
import com.doodream.rmovjs.model.RMIServiceInfo;
import com.doodream.rmovjs.model.Request;
import com.doodream.rmovjs.model.Response;
import com.doodream.rmovjs.net.ClientSocketAdapter;
import com.doodream.rmovjs.net.RMISocket;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Locale;

public class InetClientSocketAdapter implements ClientSocketAdapter {

    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(Class.class, new TypeAdapter<Class>() {
                @Override
                public void write(JsonWriter jsonWriter, Class aClass) throws IOException {
                    jsonWriter.value(aClass.getName());
                }

                @Override
                public Class read(JsonReader jsonReader) throws IOException {
                    try {
                        return Class.forName(jsonReader.nextString());
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                    return null;
                }
            }).create();

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

        String line = reader.readLine();
        this.serviceInfo = serviceInfo;

        Observable<RMIServiceInfo> handshakeRequestSingle = Observable.just(line)
                .map(s -> GSON.fromJson(s, RMIServiceInfo.class));

            Observable<Response> serviceInfoMatchedObservable = handshakeRequestSingle
                    .filter(info -> info.hashCode() == serviceInfo.hashCode())
                    .map(info -> Response.success("OK"));

            Observable<Response> serviceInfoMismatchObservable = handshakeRequestSingle
                    .filter(info -> info.hashCode() != serviceInfo.hashCode())
                    .map(info -> RMIError.FORBIDDEN.getResponse(Request.builder().serviceInfo(serviceInfo).build()));

            return serviceInfoMatchedObservable.mergeWith(serviceInfoMismatchObservable)
                    .doOnNext(this::write)
                    .map(Response::isSuccessful)
                    .doOnNext(this::setListenable)
                    .first(false).blockingGet();
    }

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
        byte[] json = Response.toJson(response).concat("\n").getBytes("UTF-8");
        client.getOutputStream().write(json);
    }

    @Override
    public Observable<Request> listen() throws IOException {
        return lineObservable.subscribeOn(Schedulers.io()).map(Request::fromJson);
    }

    @Override
    public String who() {
        return serviceInfo.getName();
    }

    @Override
    public String unique() {
        return String.format(Locale.getDefault(), "%d_%d", serviceInfo.hashCode(), client.hashCode());
    }

}
