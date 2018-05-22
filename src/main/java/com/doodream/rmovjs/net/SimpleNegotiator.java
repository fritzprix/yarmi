package com.doodream.rmovjs.net;

import com.doodream.rmovjs.model.RMIError;
import com.doodream.rmovjs.model.RMIServiceInfo;
import com.doodream.rmovjs.model.Request;
import com.doodream.rmovjs.model.Response;
import com.doodream.rmovjs.util.SerdeUtil;
import com.google.common.base.Preconditions;
import io.reactivex.Observable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;

public class SimpleNegotiator implements RMINegotiator {
    private static final Logger Log = LogManager.getLogger(SimpleNegotiator.class);

    @Override
    public RMISocket handshake(RMISocket socket, RMIServiceInfo service, boolean isClient) throws HandshakeFailException {
        Log.info("Handshake start @ {}", isClient? "CLIENT" : "SERVER");
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            Writer writer = new OutputStreamWriter(socket.getOutputStream());
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

    private void handshakeFromClient(RMIServiceInfo service, BufferedReader reader, Writer writer) throws HandshakeFailException {
        try {
            writer.write(service.toJson().concat("\n"));
            writer.flush();
            Response response = Response.fromJson(reader.readLine());
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

    private void handshakeFromServer(RMIServiceInfo service, BufferedReader reader, Writer writer) throws HandshakeFailException {
        try {
            Observable<RMIServiceInfo> handshakeRequestSingle = Observable.just(reader.readLine())
                    .map(s -> SerdeUtil.fromJson(s, RMIServiceInfo.class));

            Observable<Response> serviceInfoMatchedObservable = handshakeRequestSingle
                    .filter(info -> info.hashCode() == service.hashCode())
                    .map(info -> Response.success("OK"));

            Observable<Response> serviceInfoMismatchObservable = handshakeRequestSingle
                    .filter(info -> info.hashCode() != service.hashCode())
                    .map(info -> RMIError.FORBIDDEN.getResponse(Request.builder().build()));

            boolean success = serviceInfoMatchedObservable.mergeWith(serviceInfoMismatchObservable)
                    .doOnNext(response -> Log.info("Handshake Response : ({}) {}", response.getCode(), response.getBody()))
                    .doOnNext(response -> writer.write(SerdeUtil.toJson(response).concat("\n")))
                    .doOnNext(response -> writer.flush())
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
