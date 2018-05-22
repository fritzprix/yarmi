package com.doodream.rmovjs.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public class SerdeUtil {
    private static final Gson GSON = new GsonBuilder()
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

    public static String toJson(Object src) {
        return GSON.toJson(src);
    }

    public static byte[] toByteArray(Object src) throws UnsupportedEncodingException {
        return GSON.toJson(src).concat("\n").getBytes("UTF-8");
    }

    public static <T> T fromByteArray(byte[] data, Class<T> cls) throws UnsupportedEncodingException {
        return GSON.fromJson(new String(data, "UTF-8"), cls);
    }

    public static <T> T fromJson(JsonReader jsonReader, Class<T> cls) {
        return GSON.fromJson(jsonReader, cls);
    }

    public static <T> T fromJson(String json, Class<T> cls) {
        return GSON.fromJson(json, cls);
    }

    public static <T> T fromJson(String json, Class<T> rawClass, Class<?> parameter) {
        return GSON.fromJson(json, getType(rawClass, parameter));
    }

    static Type getType(Class<?> rawClass, Class<?> parameter) {
        return new ParameterizedType() {
            @Override
            public Type[] getActualTypeArguments() {
                return new Type[] {parameter};
            }

            @Override
            public Type getRawType() {
                return rawClass;
            }

            @Override
            public Type getOwnerType() {
                return null;
            }
        };
    }
}
