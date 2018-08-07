package com.doodream.rmovjs.net;

import com.doodream.rmovjs.model.RMIError;
import com.doodream.rmovjs.model.RMIServiceInfo;
import com.doodream.rmovjs.model.Response;
import com.doodream.rmovjs.serde.Converter;
import com.doodream.rmovjs.serde.Reader;
import com.doodream.rmovjs.serde.Writer;
import com.google.common.base.Preconditions;
import io.reactivex.Observable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class SimpleNegotiator implements RMINegotiator {
    private static final Logger Log = LoggerFactory.getLogger(SimpleNegotiator.class);

    @Override
    public RMISocket handshake(RMISocket socket, RMIServiceInfo service, Converter converter, boolean isClient) throws HandshakeFailException {
        Log.info("Handshake start @ {}", isClient? "CLIENT" : "SERVER");
        try {
            Reader reader = converter.reader(socket.getInputStream());
            Writer writer = converter.writer(socket.getOutputStream());
            if(isClient) {
                handshakeFromClient(service, reader, writer);
            } else {
                handshakeFromServer(service, reader, writer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return socket;
    }

    private void handshakeFromClient(RMIServiceInfo service, Reader reader, Writer writer) throws HandshakeFailException {
        try {
            writer.write(service);
            Response response = reader.read(Response.class);
            if ((response != null) &&
                    response.isSuccessful()) {
                Log.info("Handshake Success {} (Ver. {})", service.getName(), service.getVersion());
                return;
            }
            Preconditions.checkNotNull(response, "Response is null");
            Log.error("Handshake Fail ({}) {}",response.getCode(), response.getBody());
        } catch (IOException ignore) { }
        throw new HandshakeFailException();
    }

    private void handshakeFromServer(RMIServiceInfo service, Reader reader, Writer writer) throws HandshakeFailException {
        try {
            Observable<RMIServiceInfo> handshakeRequestSingle = Observable.just(reader.read(RMIServiceInfo.class));

            Observable<Response> serviceInfoMatchedObservable = handshakeRequestSingle
                    .filter(info -> info.hashCode() == service.hashCode())
                    .map(info -> Response.success("OK"));

            Observable<Response> serviceInfoMismatchObservable = handshakeRequestSingle
                    .filter(info -> info.hashCode() != service.hashCode())
                    .map(info -> Response.from(RMIError.BAD_REQUEST));

            boolean success = serviceInfoMatchedObservable.mergeWith(serviceInfoMismatchObservable)
                    .doOnNext(response -> Log.info("Handshake Response : ({}) {}", response.getCode(), response.getBody()))
                    .doOnNext(writer::write)
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
