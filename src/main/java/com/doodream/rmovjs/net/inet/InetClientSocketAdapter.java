package com.doodream.rmovjs.net.inet;


import com.doodream.rmovjs.model.Request;
import com.doodream.rmovjs.model.Response;
import com.doodream.rmovjs.net.*;
import com.doodream.rmovjs.util.SerdeUtil;
import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class InetClientSocketAdapter implements ClientSocketAdapter {

    private static final Logger Log = LogManager.getLogger(InetClientSocketAdapter.class);
    private RMISocket client;
    private Observable<String> lineObservable;
    private BufferedReader reader;


    InetClientSocketAdapter(RMISocket socket) throws IOException {
        client = socket;
        reader = new BufferedReader(new InputStreamReader(client.getInputStream()));
    }


    private void setListenable() {
        if(lineObservable != null) {
            return;
        }

        lineObservable = Observable.create(emitter -> {
            try {
                String line;
                while ((line = reader.readLine()) != null) {
                    emitter.onNext(line);
                }
                emitter.onComplete();
            } catch (IOException e) {
                emitter.onError(e);
            }
        });
    }


    @Override
    public void write(Response response) throws IOException {
        Log.debug("Response {}", response);
        client.getOutputStream().write(SerdeUtil.toByteArray(response));
    }

    @Override
    public Observable<Request> listen() throws IOException {
        setListenable();
        return lineObservable.subscribeOn(Schedulers.io()).map(Request::fromJson);
    }

    @Override
    public String who() {
        return client.getRemoteName();
    }

}
