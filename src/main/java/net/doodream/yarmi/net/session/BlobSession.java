package net.doodream.yarmi.net.session;

import net.doodream.yarmi.serde.Converter;
import net.doodream.yarmi.serde.Reader;
import net.doodream.yarmi.serde.Writer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Random;

public class BlobSession implements SessionHandler {

    private static final Logger Log = LoggerFactory.getLogger(BlobSession.class);

    public static final int CHUNK_MAX_SIZE_IN_BYTE = 1 << 16;
    public static final int CHUNK_MAX_SIZE_IN_CHAR = CHUNK_MAX_SIZE_IN_BYTE / Character.SIZE * Byte.SIZE;


    public static final int OP_UNSUPPORTED = -1001;
    public static final int INVALID_SIZE = -1002;
    public static final int UNEXPECTED_SEQ = -1003;

    // read error start with -2000
    public static final int SIZE_NOT_MATCHED = -2001;
    public static final int INVALID_EOS_CHAR = -2002;
    public static final BlobSession NULL = new BlobSession(null);

    private static int GLOBAL_KEY = 0;
    private static String DEFAULT_TYPE = "application/octet-stream";
    private static final Random RANDOM = new Random();

    private String key;
    private String mime;

    private transient Session session;
    private transient SessionHandler sessionHandler;

    /**
     * constructor for sender
     * @param onReady
     */
    public BlobSession(Consumer<Session> onReady) {
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
    public static BlobSession findOne(Object[] args) {
        for (Object arg : args) {
            if(arg instanceof BlobSession) {
                return (BlobSession) arg;
            }
        }
        return BlobSession.NULL;
    }


    @Override
    public void handle(SessionControlMessage scm) throws SessionControlException, IOException, IllegalAccessException, ClassNotFoundException, InstantiationException {
        sessionHandler.handle(scm);
    }

    public void start(Reader reader, Writer writer, Converter converter, SessionControlMessageWriter.Builder builder, Runnable onTeardown) {
        sessionHandler.start(reader, writer, converter, builder, onTeardown);
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

    public String getKey() {
        return key;
    }
}
