package com.doodream.rmovjs.net;

import com.doodream.rmovjs.model.RMIError;
import com.doodream.rmovjs.model.RMIServiceInfo;
import com.doodream.rmovjs.model.Response;
import com.doodream.rmovjs.serde.Converter;
import com.doodream.rmovjs.serde.Reader;
import com.doodream.rmovjs.serde.Writer;
import com.google.common.base.Preconditions;
import io.reactivex.Observable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class SimpleNegotiator implements RMINegotiator {
    private static final Logger Log = LoggerFactory.getLogger(SimpleNegotiator.class);

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
            writer.write(service);
            Log.debug("write {}", service);
            Response response = reader.read(Response.class);
            if ((response != null) &&
                    response.isSuccessful()) {
                Log.debug("Handshake Success {} (Ver. {})", service.getName(), service.getVersion());
                return;
            }
            Preconditions.checkNotNull(response, "Response is null");
            Log.error("Handshake Fail ({}) {}",response.getCode(), response.getBody());
        } catch (IOException ignore) { }
        throw new HandshakeFailException();
    }

    private void handshakeFromServer(final RMIServiceInfo service, Reader reader, final Writer writer) throws HandshakeFailException {
        try {
            Observable<RMIServiceInfo> handshakeRequestSingle = Observable.just(reader.read(RMIServiceInfo.class));

            Observable<Response> serviceInfoMatchedObservable = handshakeRequestSingle
                    .filter(new Predicate<RMIServiceInfo>() {
                        @Override
                        public boolean test(RMIServiceInfo info) throws Exception {
                            return info.hashCode() == service.hashCode();
                        }
                    })
                    .map(new Function<RMIServiceInfo, Response>() {
                        @Override
                        public Response apply(RMIServiceInfo rmiServiceInfo) throws Exception {
                            return Response.success("OK");
                        }
                    });

            Observable<Response> serviceInfoMismatchObservable = handshakeRequestSingle
                    .filter(new Predicate<RMIServiceInfo>() {
                        @Override
                        public boolean test(RMIServiceInfo info) throws Exception {
                            return info.hashCode() != service.hashCode();
                        }
                    })
                    .map(new Function<RMIServiceInfo, Response>() {
                        @Override
                        public Response apply(RMIServiceInfo rmiServiceInfo) throws Exception {
                            Log.debug("{} != {}", rmiServiceInfo, service);
                            return Response.from(RMIError.BAD_REQUEST);
                        }
                    });

            boolean success = serviceInfoMatchedObservable.mergeWith(serviceInfoMismatchObservable)
                    .doOnNext(new Consumer<Response>() {
                        @Override
                        public void accept(Response response) throws Exception {
                            Log.trace("Handshake Response : ({}) {}", response.getCode(), response.getBody());
                        }
                    })
                    .doOnNext(new Consumer<Response>() {
                        @Override
                        public void accept(Response response) throws Exception {
                            Log.debug("write response {}", response);
                            writer.write(response);
                        }
                    })
                    .map(new Function<Response, Boolean>() {
                        @Override
                        public Boolean apply(Response response) throws Exception {
                            return response.isSuccessful();
                        }
                    })
                    .filter(new Predicate<Boolean>() {
                        @Override
                        public boolean test(Boolean aBoolean) throws Exception {
                            return aBoolean;
                        }
                    })
                    .blockingSingle(false);
            if (success) {
                return;
            }
        } catch (IOException e) {
            Log.error("", e);
        }
        throw new HandshakeFailException();
    }

}
