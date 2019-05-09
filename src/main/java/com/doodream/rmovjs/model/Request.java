package com.doodream.rmovjs.model;


import com.doodream.rmovjs.annotation.RMIException;
import com.doodream.rmovjs.net.ClientSocketAdapter;
import com.doodream.rmovjs.net.ServiceProxy;
import com.doodream.rmovjs.net.session.BlobSession;
import com.doodream.rmovjs.net.session.SessionControlMessage;
import com.doodream.rmovjs.net.session.SessionControlMessageWriter;
import com.doodream.rmovjs.parameter.Param;
import com.doodream.rmovjs.serde.Writer;
import com.google.gson.annotations.SerializedName;
import io.reactivex.Observable;
import io.reactivex.functions.BiConsumer;
import io.reactivex.functions.BiFunction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Request contains information for client method invocation consisted with below
 * 1. endpoint which uniquely mapped to a method with 1:1 relation.
 * 2. parameters for method invocation
 * 3. optionally it conveys session control message which consumed by {@link com.doodream.rmovjs.net.ServiceAdapter} or {@link ServiceProxy}
 */
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class Request {

    private static final Logger Log = LoggerFactory.getLogger(Request.class);

    private transient ClientSocketAdapter client;

    private transient Response response;

    @SerializedName("session")
    private BlobSession session;

    @SerializedName("endpoint")
    private String endpoint;

    @SerializedName("params")
    private List<Param> params;

    @SerializedName("scm")
    private SessionControlMessage scm;

    @SerializedName("nonce")
    private int nonce;

    // TODO : blob header & blob

    public static boolean isValid(Request request) {
        return (request.getEndpoint() != null) &&
                (request.getParams() != null);
    }

    public static SessionControlMessageWriter buildSessionMessageWriter(final Writer writer) {
        return new SessionControlMessageWriter() {
            @Override
            public void write(SessionControlMessage controlMessage) throws IOException {
                writer.write(Request.builder()
                        .scm(controlMessage)
                        .build());
            }
        };
    }

    public synchronized void setResponse(Response response) {
        Log.trace("notify waiting thread for response ({}) : {}", nonce, response.getBody());
        this.response = response;
        this.notifyAll();
    }

    public synchronized Response getResponse(long timeout) throws InterruptedException {
        if(response != null) {
            return response;
        }
        Log.trace("wait for response ({}) with timeout {}", nonce, timeout);
        if(timeout > 0) {
            this.wait(timeout);
        } else {
            this.wait();
        }
        if(response == null) {
            Log.error("unexpected null result @ ({})", nonce);
            return RMIError.TIMEOUT.getResponse();
        }
        Log.trace("wake from waiting response ({}) @ {}", nonce, response.getBody());
        return response;
    }

    public boolean hasScm() {
        return scm != null;
    }

    public static Request fromEndpoint(Endpoint endpoint, Object ...args) {
        if(args == null) {
            return Request.builder()
                    .params(Collections.EMPTY_LIST)
                    .endpoint(endpoint.getUnique())
                    .build();
        } else {
            BlobSession session = BlobSession.findOne(args);
            final Request.RequestBuilder builder =  Request.builder()
                    .params(convertParams(endpoint, args))
                    .endpoint(endpoint.getUnique());

            if(!session.equals(BlobSession.NULL)) {
                builder.session(session);
            }

            return builder.build();
        }
    }

    private static List<Param> convertParams(final Endpoint endpoint, Object[] objects) {
        if(objects == null) {
            return Collections.EMPTY_LIST;
        }

        return Observable.fromIterable(endpoint.getParams()).zipWith(Observable.fromArray(objects), new BiFunction<Param, Object, Param>() {
            @Override
            public Param apply(Param param, Object o) throws Exception {
                param.apply(o);
                if(param.isInstanceOf(BlobSession.class)) {
                    if(o != null) {
                        endpoint.session = (BlobSession) o;
                    }
                }
                return param;
            }
        }).collectInto(new ArrayList<Param>(), new BiConsumer<ArrayList<Param>, Param>() {
            @Override
            public void accept(ArrayList<Param> params, Param param) throws Exception {
                params.add(param);
            }
        }).blockingGet();
    }
}
