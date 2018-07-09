package com.doodream.rmovjs.net.session;

import com.doodream.rmovjs.serde.Reader;
import com.doodream.rmovjs.serde.Writer;
import com.google.gson.annotations.SerializedName;
import io.reactivex.Observable;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Random;
import java.util.function.Consumer;

@Data
public class BlobSession implements SessionHandler {

    private static final Logger Log = LoggerFactory.getLogger(BlobSession.class);

    public static final byte[] CHUNK_DELIMITER = "\r\n".getBytes(StandardCharsets.UTF_8);
    public static final int CHUNK_MAX_SIZE_IN_BYTE = 64 * 1024;
    public static final int CHUNK_MAX_SIZE_IN_CHAR = CHUNK_MAX_SIZE_IN_BYTE / Character.SIZE * Byte.SIZE;


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
        Log.debug("sender session is created");
        mime = DEFAULT_TYPE;
        SenderSession senderSession = new SenderSession(onReady);
        key = senderSession.getSessionKey();
        session = senderSession;
        sessionHandler = senderSession;
    }

    /**
     * constructor for receiver
     */
    public BlobSession() {
        Log.debug("receiver session is created");
        mime = DEFAULT_TYPE;
        ReceiverSession receiverSession = new ReceiverSession();
        session = receiverSession;
        sessionHandler = receiverSession;
    }

    /**
     * get BlobSession from arguments in RMI method invocation
     * @param args arguments
     * @return
     */
    public static Optional<BlobSession> findOne(Object[] args) {
        return Observable.fromArray(args).filter(o -> o instanceof BlobSession).cast(BlobSession.class).map(Optional::ofNullable).blockingFirst(Optional.empty());
    }


    public Optional<SessionControlMessage> handle(SessionControlMessage scm, String param) throws SessionControlException, IOException {
        return sessionHandler.handle(scm, param);
    }

    public void start(Reader reader, Writer writer, SessionControlMessageWriter.Builder builder, Runnable onTeardown) {
        Log.debug("Session started {} {}", reader, writer);
        sessionHandler.start(reader, writer, builder, onTeardown);
    }

    /**
     * called by receiver
     * @throws IOException
     */
    public Session open() throws IOException {
        session.open();
        return session;
    }

    /**
     * close session from the sender side
     * @throws IOException
     */
    public void close() throws IOException {
        session.close();
    }

    public void init() {
        ((ReceiverSession) session).setSessionKey(key);
    }
}
