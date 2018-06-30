package com.doodream.rmovjs.net.session;

import com.doodream.rmovjs.serde.Reader;
import com.doodream.rmovjs.serde.Writer;
import com.google.gson.annotations.SerializedName;
import io.reactivex.Observable;
import lombok.Data;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.util.Optional;
import java.util.Random;
import java.util.function.Consumer;

@Data
public class BlobSession implements SessionHandler {
    public static final String CHUNK_DELIMITER = "\r\n";
    private static final Logger Log = LogManager.getLogger(BlobSession.class);
    static final int CHUNK_MAX_SIZE_IN_BYTE = 64 * 1024;
    static final int CHUNK_MAX_SIZE_IN_CHAR = CHUNK_MAX_SIZE_IN_BYTE / Character.SIZE * Byte.SIZE;


    public static final int OP_UNSUPPORTED = -1001;
    public static final int INVALID_SIZE = -1002;
    public static final int UNEXPECTED_SEQ = -1003;

    // read error start with -2000
    public static final int SIZE_NOT_MATCHED = -2001;
    public static final int INVALID_EOS_CHAR = -2002;

    private static int GLOBAL_KEY = 0;
    private static String DEFAULT_TYPE = "application/octet-stream";
    private static final Random RANDOM = new Random();

    @SerializedName("key")
    private String key;
    @SerializedName("mime")
    private String mime;

    private transient Session session;
    private transient SessionHandler sessionHandler;

    /**
     * constructor for sender
     * @param onReady
     */
    public BlobSession(Consumer<Session> onReady) {
        mime = DEFAULT_TYPE;
        if(onReady != null) {
            SenderSession senderSession = new SenderSession(onReady);
            key = senderSession.getSessionKey();
            session = senderSession;
            sessionHandler = senderSession;
        } else {
            ReceiverSession receiverSession = new ReceiverSession();
            receiverSession.setSessionKey(key);
            session = receiverSession;
            sessionHandler = receiverSession;
        }
    }

    /**
     * constructor for receiver
     */
    public BlobSession() {
        this(null);
    }

    public static Optional<BlobSession> findOne(Object[] args) {
        return Observable.fromArray(args).filter(o -> o instanceof BlobSession).cast(BlobSession.class).map(Optional::ofNullable).blockingFirst(Optional.empty());
    }

    public int read(byte[] b, int offset, int len) throws IOException {
        return session.read(b, offset, len);
    }

    public void handle(SessionControlMessage scm) throws IllegalStateException, IOException {
        Log.debug("scm : {}" , scm);
        sessionHandler.handle(scm);

    }

    public void start(Reader reader, Writer writer, SessionControlMessageWriter.Builder builder, Runnable onTeardown) {
        sessionHandler.start(reader, writer, builder, onTeardown);
    }

    /**
     * called by receiver
     * @throws IOException
     */
    public void open() throws IOException {
        session.open();
    }

    /**
     * close session from the sender side
     * @throws IOException
     */
    public void close() throws IOException {
        session.close();
    }
}
