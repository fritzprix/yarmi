package com.doodream.rmovjs.net.session;

import com.doodream.rmovjs.net.session.param.SCMChunkParam;
import com.doodream.rmovjs.net.session.param.SCMErrorParam;
import com.doodream.rmovjs.serde.Reader;
import com.doodream.rmovjs.serde.Writer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.OptionalLong;
import java.util.Random;
import java.util.function.Consumer;


public class SenderSession implements Session, SessionHandler {

    private static final Logger Log = LogManager.getLogger(SenderSession.class);
    private static final Random RANDOM = new Random();
    private static int GLOBAL_KEY = 0;

    private String key;
    private Consumer<Session> onReady;
    private Runnable onTeardown;
    private ByteBuffer writeBuffer;
    private SessionControlMessageWriter scmWriter;
    private int messageSeqNumber;
    private int chunkSeqNumber;

    SenderSession(Consumer<Session> onReady) {
        // create unique key for session
        OptionalLong lo = RANDOM.longs(4).reduce((left, right) -> left + right);
        if(!lo.isPresent()) {
            throw new IllegalStateException("fail to generate random");
        }

        key = Integer.toHexString(String.format("%d%d%d",GLOBAL_KEY++, System.currentTimeMillis(), lo.getAsLong()).hashCode());
        chunkSeqNumber = 0;
        messageSeqNumber = 0;
        writeBuffer = ByteBuffer.allocate(BlobSession.CHUNK_MAX_SIZE_IN_BYTE);
        this.onReady = onReady;
    }

    @Override
    public int read(byte[] b, int offset, int len) throws IOException {
        throw new NotImplementedException();
    }

    private void flushOutWriteBuffer(boolean last) throws IOException {

        CharBuffer charBuffer = writeBuffer.asCharBuffer();
        int len = writeBuffer.position() << 1;
        char[] c = new char[len];
        charBuffer.get(c);

        SessionControlMessage chunkMessage = SessionControlMessage.builder()
                .command(SessionCommand.CHUNK)
                .param(SCMChunkParam.builder().sizeInChar(len).type(last? SCMChunkParam.TYPE_LAST : SCMChunkParam.TYPE_CONTINUE))
                .key(key)
                .build();

        scmWriter.writeWithBlob(chunkMessage, writeBuffer);
        writeBuffer.position(0);
    }

    @Override
    public synchronized void write(byte[] b, int len) throws IOException {
        int available = writeBuffer.remaining();
        Log.debug("Remaining Buffer Space : {}", available);
        if(available < len) {
            // put bytes length of 'available'
            writeBuffer.put(b, 0, available - 1);
            // write buffer is full here
            Log.debug("Full write buffer size : {}", writeBuffer.position());
            flushOutWriteBuffer(false);
            // put residue to buffer
            writeBuffer.put(b, available, len - available);
        } else {
            writeBuffer.put(b, 0, len);
            if(available == len) {
                flushOutWriteBuffer(false);
            }
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
                handleErrorMessage(scm);
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
                // send error message to peer
                sendErrorMessage(command, key,"Not Supported Operation", BlobSession.OP_UNSUPPORTED);
        }
    }

    @Override
    public void start(Reader reader, Writer writer, SessionControlMessageWriter.Builder builder, Runnable onTeardown) {
        this.scmWriter = builder.build(writer);
        this.onTeardown = onTeardown;
    }

    private void handleErrorMessage(SessionControlMessage scm) {
        // TODO: handle error
        final SCMErrorParam errorParam = (SCMErrorParam) scm.getParam();
        final SCMErrorParam.ErrorType errorCode = errorParam.getType();
        Log.error(errorParam.getMsg());
        switch (errorCode) {
            case BAD_SEQUENCE:
            default:
                break;
        }
    }


    private void sendErrorMessage(SessionCommand from, String key, String msg, int err) throws IOException {
        SessionControlMessage scm = SessionControlMessage.builder().param(SCMErrorParam.build(from, msg, err)).key(key).command(from).build();
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
                .param(null)
                .build());

        onTeardown.run();
    }

    public String getSessionKey() {
        return key;
    }
}
