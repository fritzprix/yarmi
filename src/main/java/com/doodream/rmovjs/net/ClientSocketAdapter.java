package com.doodream.rmovjs.net;


import com.doodream.rmovjs.model.Request;
import com.doodream.rmovjs.model.Response;
import com.doodream.rmovjs.net.session.BlobSession;
import com.doodream.rmovjs.net.session.SessionCommand;
import com.doodream.rmovjs.net.session.SessionControlException;
import com.doodream.rmovjs.net.session.SessionControlMessage;
import com.doodream.rmovjs.net.session.param.SCMErrorParam;
import com.doodream.rmovjs.serde.Converter;
import com.doodream.rmovjs.serde.Reader;
import com.doodream.rmovjs.serde.Writer;
import io.reactivex.Observable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

public class ClientSocketAdapter {

    private Logger Log = LoggerFactory.getLogger(ClientSocketAdapter.class);

    private RMISocket client;
    private Reader reader;
    private Writer writer;
    private ConcurrentHashMap<String, BlobSession> sessionRegistry;

    ClientSocketAdapter(RMISocket socket, Converter converter) throws IOException {
        client = socket;
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

    Observable<Request> listen() {
        return Observable.create(emitter -> {
            try {
                Request request;
                while((request = reader.read(Request.class)) != null) {
                    final BlobSession session = request.getSession();
                    if(request.hasScm()) {
                        // request has session control message, route it to dedicated session
                        try {
                            // chunk scm is followed by binary stream, must be consumed properly within handleSessionControlMessage
                            Log.debug("SCM : {}", request.getScm());
                            handleSessionControlMessage(request);
                        } catch (IllegalStateException e) {
                            // dest. session doesn't exist
                            write(Response.error(request.getScm(), e.getMessage(), SCMErrorParam.ErrorType.INVALID_SESSION));
                        }
                        continue;
                    }
                    if(session != null) {
                        session.init();
                        if(sessionRegistry.put(session.getKey(), session) != null) {
                            Log.warn("session conflict");
                            return;
                        }
                        Log.debug("session registered {}", session);
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

    private void handleSessionControlMessage(Request request) throws SessionControlException, IllegalStateException, IOException {
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

    String who() {
        return client.getRemoteName();
    }

}
