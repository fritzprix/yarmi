package com.doodream.rmovjs.serde.json;

import com.doodream.rmovjs.net.session.SessionCommand;
import com.doodream.rmovjs.net.session.SessionControlMessage;
import com.doodream.rmovjs.serde.Converter;
import com.doodream.rmovjs.serde.Reader;
import com.doodream.rmovjs.serde.Writer;
import com.google.gson.*;

import javax.xml.bind.DatatypeConverter;
import java.io.*;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class JsonConverter implements Converter {

    private static class ByteArrayToBase64TypeAdapter implements JsonSerializer<byte[]>, JsonDeserializer<byte[]> {
        public byte[] deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            byte[] data = DatatypeConverter.parseBase64Binary(json.getAsString());
            return data;
        }

        public JsonElement serialize(byte[] src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(DatatypeConverter.printBase64Binary(src));
        }
    }

    private static final Gson BINARY_CAP_GSON = new GsonBuilder()
            .registerTypeHierarchyAdapter(byte[].class, new ByteArrayToBase64TypeAdapter())
            .create();

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
            }).registerTypeAdapter(SessionControlMessage.class, new TypeAdapter<SessionControlMessage<?>>() {
                @Override
                public void write(com.google.gson.stream.JsonWriter jsonWriter, SessionControlMessage<?> sessionControlMessage) throws IOException {
                    if(sessionControlMessage != null) {
                        jsonWriter.beginObject();
                        jsonWriter
                                .name("key").value(sessionControlMessage.getKey())
                                .name("cmd").value(sessionControlMessage.getCommand().name());
                        if (sessionControlMessage.getParam() != null) {
                            jsonWriter
                                    .name("param");
                            BINARY_CAP_GSON.toJson(sessionControlMessage.getParam(), sessionControlMessage.getCommand().getParamClass(), jsonWriter);
                        }
                        jsonWriter.endObject();
                    } else {
                        jsonWriter.nullValue();
                    }
                    jsonWriter.flush();
                }

                @Override
                public SessionControlMessage<?> read(com.google.gson.stream.JsonReader jsonReader) throws IOException {
                    SessionControlMessage message = SessionControlMessage.builder().build();
                    jsonReader.beginObject();
                    while (jsonReader.hasNext()) {
                        switch (jsonReader.nextName()) {
                            case "key":
                                message.setKey(jsonReader.nextString());
                                break;
                            case "cmd":
                                message.setCommand(SessionCommand.valueOf(jsonReader.nextString()));
                                break;
                            case "param":
                                Class<?> paramCls = message.getCommand().getParamClass();
                                message.setParam(BINARY_CAP_GSON.fromJson(jsonReader, paramCls));
                                break;
                        }
                    }
                    jsonReader.endObject();
                    return message;
                }
            })
            .create();



    @Override
    public Reader reader(final InputStream inputStream)  {
        return new Reader() {
            private BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            @Override
            public <T> T read(Class<T> cls) throws IOException {
                String line = reader.readLine();
                if(line == null) {
                    return null;
                }
                line = line.trim();
                return invert(StandardCharsets.UTF_8.encode(line).array(), cls);
            }
        };
    }

    @Override
    public Writer writer(final OutputStream outputStream) {
        return new Writer() {
            @Override
            public void write(Object src) throws IOException {
                outputStream.write(convert(src));
            }
        };
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
    public Object resolve(Object unresolved, Type type) {
        if(unresolved == null) {
            return null;
        }
        return GSON.fromJson(GSON.toJson(unresolved), type);
    }

    public static String toJson(Object src) {
        return GSON.toJson(src);
    }

}
