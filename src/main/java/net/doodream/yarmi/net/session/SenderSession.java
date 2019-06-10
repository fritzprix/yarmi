package net.doodream.yarmi.net.session;

import net.doodream.yarmi.net.session.param.SCMChunkParam;
import net.doodream.yarmi.net.session.param.SCMErrorParam;
import net.doodream.yarmi.serde.Converter;
import net.doodream.yarmi.serde.Reader;
import net.doodream.yarmi.serde.Writer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;


public class SenderSession implements Session, SessionHandler {

    private static final Logger Log = LoggerFactory.getLogger(SenderSession.class);
    private static final Random RANDOM = new Random();
    private static int GLOBAL_KEY = 0;
    private static final int MAX_CNWD_SIZE = 10;

    private String key;
    private Consumer<Session> onReady;
    private Runnable onTeardown;
    private Converter converter;
    private byte[] bufferSource = new byte[BlobSession.CHUNK_MAX_SIZE_IN_BYTE];
    private ByteBuffer writeBuffer;
    private SessionControlMessageWriter scmWriter;
    private int chunkSeqNumber;
    private Map<Integer, SCMChunkParam> chunkLruCache;


    SenderSession(Consumer<Session> onReady) {
        // create unique key for session

        key = String.format("%8x%8x%8x",GLOBAL_KEY++, System.currentTimeMillis(), RANDOM.nextLong()).trim();
        chunkSeqNumber = 0;
        chunkLruCache = SenderSession.getLruCache(MAX_CNWD_SIZE * 2);
        writeBuffer = ByteBuffer.wrap(bufferSource);
        this.onReady = onReady;
    }

    private static <K,V> Map<K, V> getLruCache(final int size) {
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

        SessionControlMessage chunkMessage = SessionControlMessage.builder()
                .command(SessionCommand.CHUNK)
                .key(key)
                .param(chunkParam)
                .build();

        scmWriter.write(chunkMessage);
    }

    @Override
    public synchronized void write(byte[] b, int len) throws IOException {
        int available;
        int offset = 0;
        while((available = writeBuffer.remaining()) < len) {
            writeBuffer.put(b, offset, available);
            flushOutWriteBuffer(false);
            writeBuffer.clear();
            offset += available;
            len -= available;
        }

        writeBuffer.put(b, offset, len);
        if(!writeBuffer.hasRemaining()) {
            flushOutWriteBuffer(false);
        }
    }

    @Override
    public void handle(SessionControlMessage scm) throws IllegalStateException, IOException, IllegalAccessException, InstantiationException, ClassNotFoundException {
        final SessionCommand command = scm.getCommand();
        Object param = converter.resolve(scm.getParam(), command.getParamClass());
        if(Log.isTraceEnabled()) {
            Log.trace("{} {}", command, param);
        }
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
                SCMErrorParam errorParam = (SCMErrorParam) param;
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
    public void start(Reader reader, Writer writer, Converter converter, SessionControlMessageWriter.Builder builder, Runnable onTeardown) {
        this.scmWriter = builder.build(writer);
        this.onTeardown = onTeardown;
        this.converter = converter;
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
        SessionControlMessage scm = SessionControlMessage.builder().key(key)
                .param(SCMErrorParam.build(from, msg, err))
                .command(from).build();
        scmWriter.write(scm);
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
                .build());

        onTeardown.run();
        Log.debug("closed");
    }

    public String getSessionKey() {
        return key;
    }
}
