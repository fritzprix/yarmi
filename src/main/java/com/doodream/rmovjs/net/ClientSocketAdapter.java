package com.doodream.rmovjs.net;


import com.doodream.rmovjs.model.Request;
import com.doodream.rmovjs.model.Response;
import com.doodream.rmovjs.serde.Converter;
import io.reactivex.Observable;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

public class ClientSocketAdapter {

    private RMISocket client;
    private Converter converter;
    private Reader reader;
    private Writer writer;

    ClientSocketAdapter(RMISocket socket, Converter converter) throws IOException {
        client = socket;
        this.converter = converter;
        reader = converter.reader(socket.getInputStream());
        writer = converter.writer(socket.getOutputStream());
    }


    public void write(Response response) throws IOException {
        converter.write(response, writer);
    }

    public Observable<Request> listen() throws IOException {
        return Observable.create(emitter -> {
            try {
                Request request;
                while((request = converter.read(reader, Request.class)) != null) {
                    emitter.onNext(request);
                }
                emitter.onComplete();
            } catch (IOException e) {
                emitter.onError(e);
            }
        });
    }

    public String who() {
        return client.getRemoteName();
    }

}
