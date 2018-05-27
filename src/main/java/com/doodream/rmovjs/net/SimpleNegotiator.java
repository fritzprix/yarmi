package com.doodream.rmovjs.net;

import com.doodream.rmovjs.model.RMIError;
import com.doodream.rmovjs.model.RMIServiceInfo;
import com.doodream.rmovjs.model.Response;
import com.doodream.rmovjs.serde.Converter;
import com.doodream.rmovjs.util.SerdeUtil;
import com.google.common.base.Preconditions;
import io.reactivex.Observable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;

public class SimpleNegotiator implements RMINegotiator {
    private static final Logger Log = LogManager.getLogger(SimpleNegotiator.class);

    @Override
    public RMISocket handshake(RMISocket socket, RMIServiceInfo service, Converter converter, boolean isClient) throws HandshakeFailException {
        Log.info("Handshake start @ {}", isClient? "CLIENT" : "SERVER");
        try {
            Reader reader = converter.reader(socket.getInputStream());
            Writer writer = converter.writer(socket.getOutputStream());
            if(isClient) {
                handshakeFromClient(service, reader, writer, converter);
            } else {
                handshakeFromServer(service, reader, writer, converter);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return socket;
    }

    private void handshakeFromClient(RMIServiceInfo service, Reader reader, Writer writer, Converter converter) throws HandshakeFailException {
        try {
            converter.write(service, writer);
            writer.flush();
            Response response = converter.read(reader, Response.class);
            if ((response != null) &&
                    response.isSuccessful()) {
                Log.info("Handshake Success {} (Ver. {})", service.getName(), service.getVersion());
                return;
            }
            Preconditions.checkNotNull(response, "Response is null");
            Log.error("Handshake Fail ({}) {}",response.getCode(), response.getErrorBody());
        } catch (IOException ignore) { }
        throw new HandshakeFailException();
    }

    private void handshakeFromServer(RMIServiceInfo service, Reader reader, Writer writer, Converter converter) throws HandshakeFailException {
        try {
            Observable<RMIServiceInfo> handshakeRequestSingle = Observable.just(converter.read(reader, RMIServiceInfo.class));

            Observable<Response> serviceInfoMatchedObservable = handshakeRequestSingle
                    .filter(info -> info.hashCode() == service.hashCode())
                    .map(info -> Response.success("OK"));

            Observable<Response> serviceInfoMismatchObservable = handshakeRequestSingle
                    .filter(info -> info.hashCode() != service.hashCode())
                    .map(info -> Response.from(RMIError.BAD_REQUEST));

            boolean success = serviceInfoMatchedObservable.mergeWith(serviceInfoMismatchObservable)
                    .doOnNext(response -> Log.info("Handshake Response : ({}) {}", response.getCode(), response.getBody()))
                    .doOnNext(response -> converter.write(response,writer))
                    .map(Response::isSuccessful)
                    .filter(Boolean::booleanValue)
                    .blockingSingle(false);
            if (success) {
                return;
            }
        } catch (IOException ignore) { }
        throw new HandshakeFailException();
    }

}
