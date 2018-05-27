package com.doodream.rmovjs.serde;

import com.google.common.base.Preconditions;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public class GsonConverter implements Converter {

    private static final Logger Log = LogManager.getLogger(GsonConverter.class);

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

    @Override
    public void write(Object src, Writer writer) throws IOException {
        final String line = GSON.toJson(src).concat("\n");
        writer.write(line);
        writer.flush();
    }

    @Override
    public <T> T read(Reader reader, Class<T> rawClass, Class<?> parameter) throws IOException {
        Preconditions.checkArgument(reader instanceof BufferedReader);
        final String line = ((BufferedReader) reader).readLine();
        if(line == null) {
            return null;
        }
        Log.debug("read : {}", line);
        return GSON.fromJson(line, getType(rawClass, parameter));
    }

    @Override
    public Reader reader(InputStream inputStream) {
        return new BufferedReader(new InputStreamReader(inputStream));
    }

    @Override
    public Writer writer(OutputStream outputStream) {
        return new BufferedWriter(new OutputStreamWriter(outputStream));
    }

    @Override
    public <T> T read(Reader reader, Class<T> cls) throws IOException {
        Preconditions.checkArgument(reader instanceof BufferedReader);
        final String line = ((BufferedReader) reader).readLine();
        if(line == null) {
            return null;
        }
        return GSON.fromJson(line, cls);
    }

    @Override
    public byte[] convert(Object src) throws UnsupportedEncodingException {
        return GSON.toJson(src).concat("\n").getBytes();
    }

    @Override
    public <T> T invert(byte[] b, Class<T> cls) throws UnsupportedEncodingException {
        Log.debug("invert {}", new String(b));
        // TODO : 명시적으로 텍스 트인코 딩 정의되도 록
        // TODO : line separator가 영향이 있음
        return GSON.fromJson(new String(b).trim(), cls);
    }
}
