package com.doodream.rmovjs.net;


import com.doodream.rmovjs.model.Request;
import com.doodream.rmovjs.model.Response;
import com.doodream.rmovjs.net.session.BlobSession;
import com.doodream.rmovjs.net.session.SessionCommand;
import com.doodream.rmovjs.net.session.SessionControlMessage;
import com.doodream.rmovjs.serde.Converter;
import com.doodream.rmovjs.serde.Reader;
import com.doodream.rmovjs.serde.Writer;
import io.reactivex.Observable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

public class ClientSocketAdapter {

    private Logger Log = LogManager.getLogger(ClientSocketAdapter.class);

    private RMISocket client;
    private Converter converter;
    private Reader reader;
    private Writer writer;
    private ConcurrentHashMap<String, BlobSession> sessionRegistry;

    ClientSocketAdapter(RMISocket socket, Converter converter) throws IOException {
        client = socket;
        this.converter = converter;
        sessionRegistry = new ConcurrentHashMap<>();
        reader = converter.reader(socket.getInputStream());
        writer = converter.writer(socket.getOutputStream());
    }


    public void write(Response response) throws IOException {
        if(response.isHasSessionSwitch()) {
            BlobSession session = (BlobSession) response.getBody();
            sessionRegistry.put(session.getKey(), session);
            session.start(reader, writer, Response::buildSessionMessageWriter, () -> unregisterSession(session));
        }
        writer.write(response);
    }

    public Observable<Request> listen() {
        return Observable.create(emitter -> {
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
                            write(Response.error(request.getScm(), e.getMessage(), BlobSession.OP_UNSUPPORTED));
                        }
                        continue;
                    }
                    if(session != null) {
                        if(sessionRegistry.put(session.getKey(), session) != null) {
                            Log.warn("session conflict");
                            return;
                        }
                        session.start(reader, writer, Response::buildSessionMessageWriter, () -> unregisterSession(session));
                        // forward request to transfer session object to application
                    }
                    emitter.onNext(request);
                }
                emitter.onComplete();
            } catch (IOException e) {
                emitter.onError(e);
            }
        });
    }

    private void unregisterSession(BlobSession session ) {
        if(sessionRegistry.remove(session.getKey()) == null) {
            Log.warn("fail to remove session : session not exists {}", session.getKey());
        } else {
            Log.debug("remove session : {}", session.getKey());
        }
    }

    private void handleSessionControlMessage(Request request) throws IllegalStateException, IOException {
        final SessionControlMessage scm = request.getScm();
        BlobSession session = sessionRegistry.get(scm.getKey());
        if(session == null) {
            throw new IllegalStateException("no session to handle");
        }
        if(scm.getCommand() == SessionCommand.RESET) {
            session = sessionRegistry.remove(scm.getKey());
            Log.debug("session {} teardown", session);
        }
        session.handle(scm);
    }

    public String who() {
        return client.getRemoteName();
    }

}
