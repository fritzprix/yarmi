package com.doodream.rmovjs.net.session;

import com.doodream.rmovjs.net.session.param.SCMChunkParam;
import com.doodream.rmovjs.net.session.param.SCMErrorParam;
import com.doodream.rmovjs.serde.Reader;
import com.doodream.rmovjs.serde.Writer;
import com.doodream.rmovjs.serde.json.JsonConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Consumer;


public class SenderSession implements Session, SessionHandler {

    private static final Logger Log = LoggerFactory.getLogger(SenderSession.class);
    private static final Random RANDOM = new Random();
    private static int GLOBAL_KEY = 0;
    private static final int MAX_CNWD_SIZE = 10;

    private String key;
    private Consumer<Session> onReady;
    private Runnable onTeardown;
    private byte[] bufferSource = new byte[BlobSession.CHUNK_MAX_SIZE_IN_BYTE];
    private ByteBuffer writeBuffer;
    private SessionControlMessageWriter scmWriter;
    private int chunkSeqNumber;
    private Map<Integer, SCMChunkParam> chunkLruCache;


    SenderSession(Consumer<Session> onReady) {
        // create unique key for session
        OptionalLong lo = RANDOM.longs(4).reduce((left, right) -> left + right);
        if(!lo.isPresent()) {
            throw new IllegalStateException("fail to generate random");
        }

        key = Integer.toHexString(String.format("%d%d%d",GLOBAL_KEY++, System.currentTimeMillis(), lo.getAsLong()).hashCode());
        chunkSeqNumber = 0;
        chunkLruCache = SenderSession.getLruCache(MAX_CNWD_SIZE * 2);
        writeBuffer = ByteBuffer.wrap(bufferSource);
        this.onReady = onReady;
    }

    private static <K,V> Map<K, V> getLruCache(int size) {
        return new LinkedHashMap<K, V> (size * 4/3, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry eldest) {
                return size() > size;
            }
        };
    }

    @Override
    public int read(byte[] b, int offset, int len) throws IOException {
        throw new NotImplementedException();
    }

    private void flushOutWriteBuffer(boolean last) throws IOException {
        final int len = writeBuffer.position();

        final SCMChunkParam chunkParam = SCMChunkParam.builder()
                .sizeInBytes(len)
                .sequence(chunkSeqNumber++)
                .type(last? SCMChunkParam.TYPE_LAST : SCMChunkParam.TYPE_CONTINUE)
                .data(new byte[len])
                .build();

        writeBuffer.flip();
        writeBuffer.get(chunkParam.getData());
        writeBuffer.clear();
        chunkLruCache.put(chunkSeqNumber - 1, chunkParam);
        Log.debug("size of blob : {} ", chunkParam.getData().length);

        SessionControlMessage chunkMessage = SessionControlMessage.builder()
                .command(SessionCommand.CHUNK)
                .key(key)
                .build();

        scmWriter.write(chunkMessage, chunkParam);
    }

    @Override
    public synchronized void write(byte[] b, int len) throws IOException {
        int available;
        int offset = 0;
        while((available = writeBuffer.remaining()) < len) {
            Log.debug("offset : {} / available : {}", available);
            writeBuffer.put(b, offset, available);
            flushOutWriteBuffer(false);
            writeBuffer.clear();
            offset += available;
            len -= available;
        }

        writeBuffer.put(b, 0, len);
        if(!writeBuffer.hasRemaining()) {
            flushOutWriteBuffer(false);
        }
    }

    @Override
    public void handle(SessionControlMessage scm) throws IllegalStateException, IOException {
        Log.debug("scm : {}" , scm);
        final SessionCommand command = scm.getCommand();
        switch (command) {
            case ACK:
                // ready-to-receive from receiver
                if(onReady == null) {
                    break;
                }
                // set chunk seq number into 0
                chunkSeqNumber = 0;
                // typically writing blob starts on onReady()
                onReady.accept(this);
                break;
            case ERR:
                SCMErrorParam errorParam = (SCMErrorParam) scm.getParam();
                handleErrorMessage(errorParam);
                break;
            case RESET:
                // teardown on receiver side
                cleanUp();
                if(onTeardown == null) {
                    break;
                }
                onTeardown.run();
                break;
            default:
                sendErrorMessage(command, key,"Not Supported Operation", SCMErrorParam.ErrorType.INVALID_OP);
        }
    }

    @Override
    public void start(Reader reader, Writer writer, SessionControlMessageWriter.Builder builder, Runnable onTeardown) {
        this.scmWriter = builder.build(writer);
        this.onTeardown = onTeardown;
    }

    private void handleErrorMessage(SCMErrorParam errorParam) {
        // TODO: handle error
        final SCMErrorParam.ErrorType errorCode = errorParam.getType();
        Log.error(errorParam.getMsg());
        switch (errorCode) {
            case BAD_SEQUENCE:
            default:
                break;
        }
    }


    private void sendErrorMessage(SessionCommand from, String key, String msg, SCMErrorParam.ErrorType err) throws IOException {
        SessionControlMessage scm = SessionControlMessage.builder().key(key).command(from).build();
        scmWriter.write(scm, SCMErrorParam.build(from, msg, err));
    }

    private void cleanUp() {
        // TODO : still nothing
    }

    @Override
    public void open() throws IOException {
        throw new IOException("Already Opened");
    }

    @Override
    public void close() throws IOException {
        flushOutWriteBuffer(true);
        scmWriter.write(SessionControlMessage.builder()
                .command(SessionCommand.RESET)
                .key(key)
                .build(), null);

        onTeardown.run();
        Log.debug("closed");
    }

    public String getSessionKey() {
        return key;
    }
}
