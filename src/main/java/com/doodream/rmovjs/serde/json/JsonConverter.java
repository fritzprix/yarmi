package com.doodream.rmovjs.serde.json;

import com.doodream.rmovjs.serde.Converter;
import com.doodream.rmovjs.serde.Reader;
import com.doodream.rmovjs.serde.Writer;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class JsonConverter implements Converter {

    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(Class.class, new TypeAdapter<Class>() {
                @Override
                public void write(com.google.gson.stream.JsonWriter jsonWriter, Class aClass) throws IOException {
                    jsonWriter.value(aClass.getName());
                }

                @Override
                public Class read(com.google.gson.stream.JsonReader jsonReader) throws IOException {
                    try {
                        return Class.forName(jsonReader.nextString());
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                    return null;
                }
            })
            .create();

    private static Type getType(Class<?> rawClass, Class<?> parameter) {
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


    @Override
    public Reader reader(InputStream inputStream)  {
        return new JsonReader(this, inputStream);
    }

    @Override
    public Writer writer(OutputStream outputStream) {
        return new JsonWriter(this, outputStream);
    }

    @Override
    public byte[] convert(Object src) {
        return StandardCharsets.UTF_8.encode(GSON.toJson(src).concat("\n")).array();
    }

    @Override
    public <T> T invert(byte[] b, Class<T> cls) {
        ByteBuffer buffer = ByteBuffer.wrap(b);
        return GSON.fromJson(StandardCharsets.UTF_8.decode(buffer).toString().trim(), cls);
    }

    @Override
    public <T> T invert(byte[] b, Class<T> rawClass, Class<?> parameter) {
        ByteBuffer buffer = ByteBuffer.wrap(b);
        return GSON.fromJson(StandardCharsets.UTF_8.decode(buffer).toString(), getType(rawClass, parameter));
    }
}
