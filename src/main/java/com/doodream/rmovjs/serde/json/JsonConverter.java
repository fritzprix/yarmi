package com.doodream.rmovjs.serde.json;

import com.doodream.rmovjs.net.session.SessionCommand;
import com.doodream.rmovjs.net.session.SessionControlMessage;
import com.doodream.rmovjs.serde.Converter;
import com.doodream.rmovjs.serde.Reader;
import com.doodream.rmovjs.serde.Writer;
import com.google.gson.*;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class JsonConverter implements Converter {

    private static class ByteArrayToBase64TypeAdapter implements JsonSerializer<byte[]>, JsonDeserializer<byte[]> {
        public byte[] deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            return Base64.getDecoder().decode(json.getAsString());
        }

        public JsonElement serialize(byte[] src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(Base64.getEncoder().encodeToString(src));
        }
    }
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
            .registerTypeAdapter(SessionControlMessage.class, new TypeAdapter<SessionControlMessage>() {
                @Override
                public void write(com.google.gson.stream.JsonWriter jsonWriter, SessionControlMessage controlMessage) throws IOException {
                    jsonWriter.
                }

                @Override
                public SessionControlMessage read(com.google.gson.stream.JsonReader jsonReader) throws IOException {
                    String line = jsonReader.nextString();
                    JsonPrimitive msg = new JsonPrimitive(line);
                    JsonObject msgObject = msg.getAsJsonObject();
                    JsonElement command = msgObject.get("cmd");
                    JsonObject cmdObject = command.getAsJsonObject();
                    cmdObject.getAsString();
                    // TODO : parse command
                    return null;
                }
            })
            .registerTypeHierarchyAdapter(byte[].class, new ByteArrayToBase64TypeAdapter())
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

    public static String toJson(Object src) {
        return GSON.toJson(src);
    }

    public static <T> T fromJson(String json, Class<T> rawClass, Class<?> parameter) {
        return GSON.fromJson(json, getType(rawClass, parameter));
    }

    public static <T> T fromJson(String json, Class<T> rawClass) {
        return GSON.fromJson(json, rawClass);
    }
}
