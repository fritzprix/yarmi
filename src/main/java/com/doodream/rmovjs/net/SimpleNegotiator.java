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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class SimpleNegotiator implements Negotiator {
    private static final Logger Log = LoggerFactory.getLogger(SimpleNegotiator.class);
    private static final long MAX_TIMEOUT = 10L;

    @Override
    public RMISocket handshake(RMISocket socket, RMIServiceInfo service, Converter converter, boolean isClient) throws HandshakeFailException {
        Log.info("Handshake start as {} @ {}", isClient? "CLIENT" : "SERVER", socket.getRemoteName());
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

    private void handshakeFromClient(final RMIServiceInfo service, Reader reader, Writer writer) throws HandshakeFailException {
        try {
            writer.write(service, MAX_TIMEOUT, TimeUnit.SECONDS);
            Log.debug("write {}", service);
            Response response = reader.read(Response.class,MAX_TIMEOUT, TimeUnit.SECONDS);
            if ((response != null) &&
                    response.isSuccessful()) {
                Log.debug("Handshake Success {} (Ver. {})", service.getName(), service.getVersion());
                return;
            }
            Preconditions.checkNotNull(response, "Response is null");
            Log.error("Handshake Fail ({}) {}",response.getCode(), response.getBody());
        } catch (IOException e) {
            Log.error("error on read : {}", e.getMessage());
        } catch (TimeoutException e) {
            Log.error("timeout on read : {}", e.getMessage());
        }
        throw new HandshakeFailException();
    }

    private void handshakeFromServer(final RMIServiceInfo service, Reader reader, final Writer writer) throws HandshakeFailException {
        try {
            final RMIServiceInfo serviceInfo = reader.read(RMIServiceInfo.class, MAX_TIMEOUT, TimeUnit.SECONDS);

            Observable<RMIServiceInfo> handshakeRequestSingle = Observable.just(serviceInfo);
            Observable<Response> serviceInfoMatchedObservable = handshakeRequestSingle
                    .filter(info -> info.hashCode() == service.hashCode())
                    .map(rmiServiceInfo -> Response.success("OK"));

            Observable<Response> serviceInfoMismatchObservable = handshakeRequestSingle
                    .filter(info -> info.hashCode() != service.hashCode())
                    .map(rmiServiceInfo -> {
                        Log.debug("{} != {}", rmiServiceInfo, service);
                        return Response.from(RMIError.BAD_REQUEST);
                    });

            boolean success = serviceInfoMatchedObservable.mergeWith(serviceInfoMismatchObservable)
                    .doOnNext(response -> Log.trace("Handshake Response : ({}) {}", response.getCode(), response.getBody()))
                    .doOnNext(response -> {
                        Log.debug("write response {}", response);
                        writer.write(response, MAX_TIMEOUT, TimeUnit.SECONDS);
                    })
                    .map(response -> response.isSuccessful())
                    .filter(aBoolean -> aBoolean)
                    .blockingSingle(false);
            if (success) {
                return;
            }
        } catch (IOException e) {
            Log.error("error on handshake : {}", e.getMessage());
        } catch (TimeoutException e) {
            Log.error("timeout on handshake : {}", e.getMessage());
        }
        throw new HandshakeFailException();
    }

}
