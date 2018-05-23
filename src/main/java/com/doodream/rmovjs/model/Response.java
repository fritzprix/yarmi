package com.doodream.rmovjs.model;

import com.doodream.rmovjs.net.RMISocket;
import com.doodream.rmovjs.util.SerdeUtil;
import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 * Created by innocentevil on 18. 5. 4.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Response<T> {

    Endpoint endpoint;
    Class bodyCls;
    T body;
    boolean isSuccessful;
    ResponseBody errorBody;
    int code;

    static Response from(Response response) {
        return Response.builder()
                .endpoint(response.endpoint)
                .body(response.body)
                .bodyCls(response.bodyCls)
                .isSuccessful(response.isSuccessful)
                .code(response.code)
                .build();
    }

    public static <T> Response<T> success(T body, Class<T> cls) {
        return Response.<T>builder()
                .body(body)
                .code(200)
                .isSuccessful(true)
                .bodyCls(cls)
                .build();
    }

    public static Response fromJson(String json) {
        return SerdeUtil.fromJson(json, Response.class);
    }

    public static Response fromJson(String json, Class cls) {
        return SerdeUtil.fromJson(json, Response.class, cls);
    }

    public static Response error(int code, String mesg) {
        return Response.<ResponseBody>builder()
                .code(code)
                .isSuccessful(false)
                .errorBody(new ResponseBody(mesg))
                .build();
    }

    public static Response success(String msg) {
        return Response.builder()
                .code(Code.SUCCESS)
                .isSuccessful(true)
                .body(new ResponseBody(msg))
                .bodyCls(ResponseBody.class)
                .build();
    }

    public static Response from(RMIError error) {
        // TODO : consider the case which the endpoinst is not applicable (e.g. handshake response)
        return error.getResponse();
    }

    public void to(RMISocket client) throws IOException {
        client.getOutputStream().write(SerdeUtil.toByteArray(this));
    }

    @Data
    public static class ResponseBody {
        @SerializedName("msg")
        String message;

        private ResponseBody(String msg) {
            message = msg;
        }
    }

    public static class Code {
        public static final int SUCCESS = 200;
    }

}
