package com.doodream.rmovjs.model;

import com.doodream.rmovjs.net.SerdeUtil;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.IOException;

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
    int code;

    public static Response from(Response response) {
        return Response.builder()
                .endpoint(response.endpoint)
                .body(response.body)
                .bodyCls(response.bodyCls)
                .isSuccessful(response.isSuccessful)
                .code(response.code)
                .build();
    }

    public static <T> Response<T> build(T body, Class<T> cls) {
        return Response.<T>builder()
                .body(body)
                .bodyCls(cls)
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

    public static Response success(Request request, Response response) {
        response.setSuccessful(true);
        response.setCode(200);
        response.setEndpoint(request.getEndpoint());
        response.setBodyCls(request.getEndpoint().responseType);
        return response;
    }



    public static String toJson(Response response) {
        return SerdeUtil.toJson(response);
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
                .body(new ResponseBody(mesg))
                .bodyCls(ResponseBody.class)
                .build();
    }

    public static Response success(String msg) {
        return Response.builder()
                .code(200)
                .isSuccessful(true)
                .body(new ResponseBody(msg))
                .bodyCls(ResponseBody.class)
                .build();
    }

    @Data
    public static class ResponseBody {
        @SerializedName("msg")
        String message;

        private ResponseBody(String msg) {
            message = msg;
        }

        public static String toJson(ResponseBody errorBody) {
            return new Gson().toJson(errorBody);
        }

        public static ResponseBody fromJson(String json) {
            return new Gson().fromJson(json, ResponseBody.class);
        }
    }
}
