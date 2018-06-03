package com.doodream.rmovjs.net.session;

import com.google.gson.annotations.SerializedName;
import io.reactivex.Observable;
import lombok.Data;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Random;

@Data
public class BlobSession {
    private static final Logger Log = LogManager.getLogger(BlobSession.class);
    private static int GLOBAL_KEY = 0;
    private static String DEFAULT_TYPE = "application/octet-stream";
    private static final Random RANDOM = new Random();

    @SerializedName("key")
    private String key;
    @SerializedName("mime")
    private String mime;
    private transient Runnable onReady;
    private transient Runnable onTeardown;
    private transient Reader reader;
    private transient SessionControlMessageWriter writer;

    public BlobSession() {
        OptionalLong lo = RANDOM.longs(4).reduce((left, right) -> left + right);
        if(!lo.isPresent()) {
            throw new IllegalStateException("fail to generate random");
        }
        key = Integer.toHexString(String.format("%d%d%d",GLOBAL_KEY++, System.currentTimeMillis(), lo.getAsLong()).hashCode());
        mime = DEFAULT_TYPE;
    }

    public static Optional<BlobSession> findOne(Object[] args) {
        return Observable.fromArray(args).filter(o -> o instanceof BlobSession).cast(BlobSession.class).map(Optional::ofNullable).blockingFirst(Optional.empty());
    }

    public void onReady(Runnable action) {
        onReady = action;
    }

    public void handle(SessionControlMessage scm) throws IllegalStateException {
        Log.debug("scm : {}" , scm);
        switch (scm.getCommand()) {
            case ACK:
                onReady.run();
                break;
            case CHUNK:
                break;
            case ECHO:
                break;
            case ECHOBACK:
                break;
            case ERR:
                break;
            case RESET:
                onTeardown.run();
                break;
            default:
                throw new IllegalStateException("");
        }
    }

    public void start(Reader reader, SessionControlMessageWriter writer, Runnable onTeardown) {
        this.reader = reader;
        this.writer = writer;
        this.onTeardown = onTeardown;
    }

    public void open() throws IOException {
        writer.write(SessionControlMessage.builder().command(SessionCommand.ACK).key(key).param(null).build());
        // TODO : prepare blob buffered stream
    }

    public void close() throws IOException {
        // TODO : close blob buffered stream
        writer.write(SessionControlMessage.builder()
                .command(SessionCommand.RESET)
                .key(key)
                .param(null)
                .build());
        onTeardown.run();
    }
}
