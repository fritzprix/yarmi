package net.doodream.yarmi.net;


import net.doodream.yarmi.model.Request;
import net.doodream.yarmi.model.Response;
import net.doodream.yarmi.net.session.BlobSession;
import net.doodream.yarmi.net.session.SessionControlException;
import net.doodream.yarmi.net.session.SessionControlMessage;
import net.doodream.yarmi.net.session.param.SCMErrorParam;
import net.doodream.yarmi.serde.Converter;
import net.doodream.yarmi.serde.Reader;
import net.doodream.yarmi.serde.Writer;
import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ConcurrentHashMap;

public class ClientSocketAdapter {

    private Logger Log = LoggerFactory.getLogger(ClientSocketAdapter.class);

    private RMISocket client;
    private Reader reader;
    private Writer writer;
    private Converter converter;
    private final ConcurrentHashMap<String, BlobSession> sessionRegistry;

    ClientSocketAdapter(RMISocket socket, InputStream in, OutputStream out, Converter converter) {
        client = socket;
        sessionRegistry = new ConcurrentHashMap<>();
        reader = converter.reader(in);
        writer = converter.writer(out);
        this.converter = converter;
    }

    public static ClientSocketAdapter create(final RMISocket client, final Converter converter) throws IOException {
        final OutputStream os = client.getOutputStream();
        final InputStream is = client.getInputStream();
        return new ClientSocketAdapter(client, is, os, converter);
    }


    public void write(Response response) throws Exception {
        if(response.hasSessionSwitch()) {
            BlobSession session = (BlobSession) response.getBody();
            sessionRegistry.put(session.getKey(), session);
            session.start(reader, writer, converter, Response::buildSessionMessageWriter, () -> unregisterSession(session));
        }
        writer.write(response);
    }

    Observable<Request> listen() {
        Observable<Request> requestObservable = Observable.create(emitter -> {
            try {
                Request request;
                while((request = reader.read(Request.class)) != null) {
                    final BlobSession session = request.getSession();
                    if(request.hasScm()) {
                        // request has session control message, route it to dedicated session
                        try {
                            // chunk scm is followed by binary stream, must be consumed properly within handleSessionControlMessage
                            handleSessionControlMessage(request);
                        } catch (IllegalStateException e) {
                            // dest. session doesn't exist
                            write(Response.error(request.getScm(), e.getMessage(), SCMErrorParam.ErrorType.INVALID_SESSION));
                        }
                        continue;
                    }
                    if(session != null) {
                        session.init();
                        if (sessionRegistry.put(session.getKey(), session) != null) {
                            Log.warn("session conflict");
                            return;
                        }
                        Log.debug("session registered {}", session);
                        session.start(reader, writer, converter, Response::buildSessionMessageWriter, () -> unregisterSession(session));
                        // forward request to transfer session object to application
                    }
                    emitter.onNext(request);
                }
                emitter.onComplete();
            } catch (IOException e) {
                client.close();
            }
        });
        return requestObservable.subscribeOn(Schedulers.io());
    }

    private void unregisterSession(BlobSession session ) {
        if (sessionRegistry.remove(session.getKey()) == null) {
            Log.warn("fail to remove session : session not exists {}", session.getKey());
            return;
        }
        Log.trace("remove session : {}", session.getKey());
    }

    private void handleSessionControlMessage(Request request) throws SessionControlException, IllegalStateException, IOException, IllegalAccessException, InstantiationException, ClassNotFoundException {
        final SessionControlMessage scm = request.getScm();
        BlobSession session;
        session = sessionRegistry.get(scm.getKey());
        if(session == null) {
            Log.warn("no session to handle {}", scm.getKey());
            throw new IllegalStateException("no session to handle");
        }
        session.handle(scm);
    }

    String who() {
        return client.getRemoteName();
    }

    public void close() throws IOException {
        client.close();
        Log.debug("client closed");
    }
}
