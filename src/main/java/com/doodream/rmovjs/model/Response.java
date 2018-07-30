package com.doodream.rmovjs.model;

import com.doodream.rmovjs.net.session.BlobSession;
import com.doodream.rmovjs.net.session.SessionCommand;
import com.doodream.rmovjs.net.session.SessionControlMessage;
import com.doodream.rmovjs.net.session.SessionControlMessageWriter;
import com.doodream.rmovjs.net.session.param.SCMErrorParam;
import com.doodream.rmovjs.serde.Converter;
import com.doodream.rmovjs.serde.Writer;
import com.doodream.rmovjs.serde.json.JsonConverter;
import com.google.common.base.Preconditions;
import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;

/**
 * Created by innocentevil on 18. 5. 4.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Response<T> {
    private static Logger Log = LoggerFactory.getLogger(Response.class);
    public static final int SUCCESS = 200;

    private String endpoint;
    private T body;
    private boolean isSuccessful;
    private boolean hasSessionSwitch;
    private ResponseBody errorBody;
    private int code;
    private int nonce;
    @SerializedName("scm")
    private SessionControlMessage scm;

    public static <T> Response<T> success(T body) {
        return Response.<T>builder()
                .body(body)
                .code(SUCCESS)
                .isSuccessful(true)
                .build();
    }

    public static Response<BlobSession> success(BlobSession session) {
        return Response.<BlobSession>builder()
                .body(session)
                .code(SUCCESS)
                .isSuccessful(true)
                .hasSessionSwitch(true)
                .build();
    }

    public T getBody() {
        return body;
    }

    public void setBody(T body) {
        this.body = body;
    }

    public static Response error(int code, String msg) {
        return Response.<ResponseBody>builder()
                .code(code)
                .isSuccessful(false)
                .errorBody(new ResponseBody(msg))
                .build();
    }

    public static Response error(SessionControlMessage scm, String msg, SCMErrorParam.ErrorType type) {
        SessionControlMessage controlMessage = SessionControlMessage.<SCMErrorParam>builder()
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

    public static Response success(String msg) {
        ResponseBody body = new ResponseBody(msg);
        return Response.builder()
                .code(SUCCESS)
                .isSuccessful(true)
                .body(JsonConverter.toJson(body))
                .build();
    }

    public static Response from(RMIError error) {
        return error.getResponse();
    }

    public static void validate(Response res) {
        if(res.code == Response.SUCCESS) {
            Preconditions.checkNotNull(res.getBody(), "Successful response must have non-null body");
        } else {
            Preconditions.checkNotNull(res.getErrorBody(), "Error response must have non-null error body");
        }
    }

    public static SessionControlMessageWriter buildSessionMessageWriter(Writer writer) {
        return (controlMessage) -> {
            writer.write(Response.builder()
                    .scm(controlMessage)
                    .build());

        };
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
     * this method is called when response is received from the server by {@link com.doodream.rmovjs.net.BaseServiceProxy}
     *
     * @param converter converter implementation
     * @param type {@link Type} for body content
     */
    public void resolve(Converter converter, Type type) {
        setBody(converter.resolve(getBody(), type));
    }
}
