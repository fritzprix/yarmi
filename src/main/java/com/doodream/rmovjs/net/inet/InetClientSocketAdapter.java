package com.doodream.rmovjs.net.inet;


import com.doodream.rmovjs.model.RMIError;
import com.doodream.rmovjs.model.RMIServiceInfo;
import com.doodream.rmovjs.model.Request;
import com.doodream.rmovjs.model.Response;
import com.doodream.rmovjs.net.ClientSocketAdapter;
import com.doodream.rmovjs.net.HandshakeFailException;
import com.doodream.rmovjs.net.RMISocket;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import io.reactivex.Observable;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Locale;
import java.util.Scanner;

public class InetClientSocketAdapter implements ClientSocketAdapter {

    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(Class.class, new TypeAdapter<Class>() {
                @Override
                public void write(JsonWriter jsonWriter, Class aClass) throws IOException {
                    jsonWriter.value(aClass.getCanonicalName());
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
    private OutputStreamWriter writer;


    InetClientSocketAdapter(RMISocket socket) throws IOException {
        client = socket;
        writer = new OutputStreamWriter(socket.getOutputStream());
    }


    private Observable<String> listenLine() throws IOException {
        Scanner reader = new Scanner(client.getInputStream());
        return Observable.create(emitter -> {
            try {
                while (reader.hasNext()) {
                    emitter.onNext(reader.nextLine());
                }
                emitter.onComplete();
            } catch (Exception e) {
                emitter.onError(e);
            }
        });
    }

    @Override
    public void handshake(RMIServiceInfo serviceInfo) throws HandshakeFailException {
        try {
            this.serviceInfo = serviceInfo;
            writer.write(GSON.toJson(serviceInfo));

            Observable<RMIServiceInfo> serviceInfoObservable = listenLine()
                    .map(s -> GSON.fromJson(s, RMIServiceInfo.class));

            serviceInfoObservable
                    .filter(info -> info.hashCode() == serviceInfo.hashCode())
                    .subscribe();

            serviceInfoObservable
                    .filter(info -> info.hashCode() != serviceInfo.hashCode())
                    .doOnNext(info -> {
                        Response resp = RMIError.FORBIDDEN.getResponse(Request.builder().serviceInfo(serviceInfo).build());
                        writer.write(GSON.toJson(resp));
                    })
                    .subscribe();

        } catch (IOException e) {
            throw new HandshakeFailException(this);
        }
    }

    @Override
    public void write(Response response) throws IOException {
        writer.write(Response.toJson(response));
    }

    @Override
    public Observable<Request> listen() throws IOException {
        return listenLine().map(Request::fromJson);
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
