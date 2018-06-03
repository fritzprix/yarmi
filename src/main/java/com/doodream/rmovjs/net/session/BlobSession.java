package com.doodream.rmovjs.net.session;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.io.Reader;
import java.util.OptionalLong;
import java.util.Random;

@Data
public class BlobSession {
    private static int GLOBAL_KEY = 0;
    private static String DEFAULT_TYPE = "application/octet-stream";
    private static final Random RANDOM = new Random();

    @SerializedName("key")
    private String key;
    @SerializedName("mime")
    private String mime;

    public BlobSession() {
        OptionalLong lo = RANDOM.longs(4).reduce((left, right) -> left + right);
        if(!lo.isPresent()) {
            throw new IllegalStateException("fail to generate random");
        }
        key = Integer.toHexString(String.format("%d%d%d",GLOBAL_KEY++, System.currentTimeMillis(), lo.getAsLong()).hashCode());
        mime = DEFAULT_TYPE;
    }

    public void handle(SessionControlMessage scm) throws IllegalStateException {

    }

    public void start(Reader reader, SessionControlMessageWriter writer, Runnable onTeardown) {

    }

    public void close() {

    }
}
