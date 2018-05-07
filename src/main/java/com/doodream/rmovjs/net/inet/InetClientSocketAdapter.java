package com.doodream.rmovjs.net.inet;


import com.doodream.rmovjs.model.ErrorMessage;
import com.doodream.rmovjs.model.Request;
import com.doodream.rmovjs.model.Response;
import com.doodream.rmovjs.model.ServiceInfo;
import com.doodream.rmovjs.net.ClientSocketAdapter;
import com.doodream.rmovjs.net.HandshakeFailException;
import com.doodream.rmovjs.net.RMISocket;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;

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

    private ServiceInfo serviceInfo;
    private RMISocket client;
    private OutputStreamWriter writer;
    private PublishSubject<String> clientRequestSubject = PublishSubject.create();


    InetClientSocketAdapter(RMISocket socket) throws IOException {
        client = socket;
        writer = new OutputStreamWriter(socket.getOutputStream());
    }


    private Observable<String> listenLine() throws IOException {

        Scanner reader = new Scanner(client.getInputStream());
        new Thread(() -> {
            String line;
            while((line = reader.nextLine()) != null) {
                clientRequestSubject.onNext(line);
            }
            clientRequestSubject.onComplete();
        }).start();

        return Observable.fromPublisher(clientRequestSubject.toFlowable(BackpressureStrategy.BUFFER));
    }

    @Override
    public void handshake(ServiceInfo serviceInfo) throws HandshakeFailException {
        try {
            writer.write(GSON.toJson(serviceInfo));
            this.serviceInfo = listenLine()
                    .map(s -> GSON.fromJson(s, ServiceInfo.class))
                    .blockingFirst();
            if(serviceInfo.hashCode() != this.serviceInfo.hashCode()) {
                Response<ErrorMessage> resp = Response.build(ErrorMessage.builder()
                        .code(403)
                        .msg("service info is not matched")
                        .build(),ErrorMessage.class);

                writer.write(GSON.toJson(resp));
            }

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
