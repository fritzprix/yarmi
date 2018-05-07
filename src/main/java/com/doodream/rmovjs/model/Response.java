package com.doodream.rmovjs.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
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
                    jsonWriter.value(aClass.getCanonicalName());
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
    Class<T> bodyCls;
    T body;
    boolean isSuccessful;
    int code;

    public static String toJson(Response response) {
        return GSON.toJson(response);
    }

    public static Response fromJson(String json) {
        return GSON.fromJson(json, Response.class);
    }
}
