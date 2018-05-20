package com.doodream.rmovjs.model;

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
    private final static Gson GSON = new GsonBuilder()
            .registerTypeAdapter(Class.class, new TypeAdapter<Class>() {
                @Override
                public void write(JsonWriter jsonWriter, Class aClass) throws IOException {
                    jsonWriter.value(aClass.getName());
                }

                @Override
                public Class read(JsonReader jsonReader) throws IOException {
                    try {
                        return Class.forName(jsonReader.nextString());
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                    return null;
                }
            })
            .create();

    public static <T> Response<T> build(T body, Class<T> cls) {
        return Response.<T>builder()
                .body(body)
                .bodyCls(cls)
                .build();
    }
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

    public static Response success(Request request, Response response) {
        response.setSuccessful(true);
        response.setCode(200);
        response.setEndpoint(request.getEndpoint());
        response.setBodyCls(request.getEndpoint().responseType);
        return response;
    }



    public static String toJson(Response response) {
        return GSON.toJson(response);
    }

    public static Response fromJson(String json) {
        return GSON.fromJson(json, Response.class);
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
