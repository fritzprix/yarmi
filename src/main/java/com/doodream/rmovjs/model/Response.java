package com.doodream.rmovjs.model;

import com.doodream.rmovjs.net.SimpleServiceProxy;
import com.doodream.rmovjs.net.session.BlobSession;
import com.doodream.rmovjs.net.session.SessionCommand;
import com.doodream.rmovjs.net.session.SessionControlMessage;
import com.doodream.rmovjs.net.session.SessionControlMessageWriter;
import com.doodream.rmovjs.net.session.param.SCMErrorParam;
import com.doodream.rmovjs.serde.Converter;
import com.doodream.rmovjs.serde.Writer;
import com.doodream.rmovjs.util.Types;
import com.google.common.base.Preconditions;
import com.google.gson.annotations.SerializedName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * Created by innocentevil on 18. 5. 4.
 */
public class Response<T> {
    private static Logger Log = LoggerFactory.getLogger(Response.class);
    public static final int SUCCESS = 200;

    private String endpoint;
    private T body;
    private boolean isSuccessful;
    private boolean hasSessionSwitch;
    private int code;
    private int nonce;
    @SerializedName("scm")
    private SessionControlMessage scm;

    public boolean hasSessionSwitch() {
        return hasSessionSwitch;
    }


    public static class Builder<T> {

        private final Response<T> response = new Response<>();

        public Builder<T> isSuccessful(boolean success) {
            response.isSuccessful = success;
            return this;
        }

        public Builder<T> body(T body) {
            response.body = body;
            return this;
        }

        public Builder<T> code(int code) {
            response.code = code;
            return this;
        }

        public Builder<T> hasSessionSwitch(boolean hasSessionSwitch) {
            response.hasSessionSwitch = hasSessionSwitch;
            return this;
        }

        public Builder<T> scm(SessionControlMessage scm) {
            response.scm = scm;
            return this;
        }

        public Response<T> build() {
            return response;
        }
    }

    public static <T> Response<T> success(T body) {
        return Response.<T>builder()
                .body(body)
                .code(SUCCESS)
                .hasSessionSwitch(body instanceof BlobSession)
                .isSuccessful(true)
                .build();
    }

    private static <T> Builder<T> builder() {
        return new Builder<>();
    }

    public T getBody() {
        return body;
    }

    public void setBody(T body) {
        this.body = body;
    }

    public static Response error(int code, String msg) {
        return Response.<String>builder()
                .isSuccessful(false)
                .code(code)
                .body(msg)
                .build();
    }

    public static Response error(SessionControlMessage scm, String msg, SCMErrorParam.ErrorType type) {
        SessionControlMessage<SCMErrorParam> controlMessage = SessionControlMessage.<SCMErrorParam>builder()
                .key(scm.getKey())
                .command(SessionCommand.ERR)
                .param(SCMErrorParam.build(scm.getCommand(), msg, type))
                .build();


        return Response.builder()
                .code(600)
                .isSuccessful(false)
                .scm(controlMessage)
                .build();
    }

    public static Response from(RMIError error) {
        return error.getResponse();
    }

    public static void validate(Response res) {
        Preconditions.checkNotNull(res.getBody(), "Successful response must have non-null body");
        if(!res.isSuccessful) {
            Preconditions.checkArgument(res.getBody() instanceof String, "Error response must have non-null error body");
        }
    }

    public static SessionControlMessageWriter buildSessionMessageWriter(final Writer writer) {
        return new SessionControlMessageWriter() {
            @Override
            public void write(SessionControlMessage controlMessage) throws IOException {
                writer.write(Response.builder().scm(controlMessage).build());
            }
        };
    }

    public void setNonce(int nonce) {
        this.nonce = nonce;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public SessionControlMessage getScm() {
        return scm;
    }

    public int getNonce() {
        return nonce;
    }

    public boolean isSuccessful() {
        return isSuccessful;
    }

    public int getCode() {
        return code;
    }

    /**
     * check whether this response contains scm ({@link SessionControlMessage}) or not
     * @return true if scm is not null, otherwise false
     */
    public boolean hasScm() {
        return scm != null;
    }

    /**
     * response is generic container and conveys no type information in itself, which means any deserializer
     * would be not able to handle deserialization of the body field, which is generic, properly.
     * this method is called when response is received from the server by {@link SimpleServiceProxy}
     *
     * @param converter converter implementation
     * @param type {@link Type} for body content
     */
    public void resolve(Converter converter, Type type) throws IllegalAccessException, InstantiationException, ClassNotFoundException {
        if(!(type instanceof ParameterizedType)) {
            Class rawCls = Class.forName(Types.getTypeName(type));
            if(Types.isCastable(body, rawCls)) {
                return;
            }
        }

        setBody((T) converter.resolve(getBody(), type));
    }

}
