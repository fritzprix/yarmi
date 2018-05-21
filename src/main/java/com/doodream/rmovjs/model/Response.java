package com.doodream.rmovjs.model;

import com.doodream.rmovjs.net.SerdeUtil;
import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    ErrorBody errorBody;
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
        return Response.<ErrorBody>builder()
                .code(code)
                .isSuccessful(false)
                .errorBody(new ErrorBody(mesg))
                .build();
    }

    public static Response success(String msg) {
        return Response.builder()
                .code(Code.SUCCESS)
                .isSuccessful(true)
                .body(new ErrorBody(msg))
                .bodyCls(ErrorBody.class)
                .build();
    }

    @Data
    public static class ErrorBody {
        @SerializedName("msg")
        String message;

        private ErrorBody(String msg) {
            message = msg;
        }
    }

    public static class Code {

        public static final int SUCCESS = 200;
    }
}
