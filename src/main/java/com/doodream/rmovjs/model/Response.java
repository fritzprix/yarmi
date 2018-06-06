package com.doodream.rmovjs.model;

import com.doodream.rmovjs.net.session.BlobSession;
import com.doodream.rmovjs.net.session.SessionCommand;
import com.doodream.rmovjs.net.session.SessionControlMessage;
import com.doodream.rmovjs.net.session.SessionControlMessageWriter;
import com.doodream.rmovjs.net.session.param.SCMErrorParam;
import com.doodream.rmovjs.serde.Converter;
import com.google.common.base.Preconditions;
import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.io.Writer;

/**
 * Created by innocentevil on 18. 5. 4.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Response<T> {
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

    public static Response error(int code, String mesg) {
        return Response.<ResponseBody>builder()
                .code(code)
                .isSuccessful(false)
                .errorBody(new ResponseBody(mesg))
                .build();
    }

    public static Response error(SessionControlMessage scm, String msg) {
        SessionControlMessage<SCMErrorParam> controlMessage = SessionControlMessage.<SCMErrorParam>builder()
                .key(scm.getKey())
                .command(SessionCommand.ERR)
                .param(SCMErrorParam.build(scm.getCommand(), msg))
                .build();

        return Response.builder()
                .code(600)
                .isSuccessful(false)
                .scm(controlMessage)
                .build();
    }

    public static Response success(String msg) {
        return Response.builder()
                .code(SUCCESS)
                .isSuccessful(true)
                .body(new ResponseBody(msg))
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

    public static SessionControlMessageWriter buildSessionMessageWriter(Writer writer, Converter converter) {
        // TODO : return SessionControlMessageWriter
        return new SessionControlMessageWriter() {
            @Override
            public void write(SessionControlMessage controlMessage) throws IOException {
                converter.write(Response.builder().scm(controlMessage).build(), writer);
            }
        };
    }

    public boolean hasScm() {
        return scm != null;
    }

}
