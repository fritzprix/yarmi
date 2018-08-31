package com.doodream.rmovjs.net.session;

import com.doodream.rmovjs.net.session.param.SCMChunkParam;
import com.doodream.rmovjs.net.session.param.SCMErrorParam;
import com.doodream.rmovjs.serde.Converter;
import com.doodream.rmovjs.serde.Reader;
import com.doodream.rmovjs.serde.Writer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ReceiverSession implements Session, SessionHandler {

    private static final Logger Log = LoggerFactory.getLogger(ReceiverSession.class);
    private String key;
//    private InputStream chunkInStream;
//    private OutputStream chunkOutStream;
    private final ConcurrentLinkedQueue<byte[]> dataQueue = new ConcurrentLinkedQueue<>();
    private volatile boolean EOS;
    private ByteBuffer cBuffer;

    private Converter converter;
    private Runnable onTeardown;
    private long overallRcvSize;
    private SessionControlMessageWriter scmWriter;

    ReceiverSession() {
        overallRcvSize = 0L;
    }

    @Override
    public int read(byte[] b, int offset, int len) {
        if(cBuffer == null) {
            byte[] data;
            while((data = dataQueue.poll()) == null) {
                if(EOS) {
                    return -1;
                }
                synchronized (dataQueue) {
                    try {
                        dataQueue.wait(100L);
                    } catch (InterruptedException e) {
                        return -1;
                    }
                }
            }
            cBuffer = ByteBuffer.wrap(data);
        }
        final int rsz = cBuffer.remaining();
        if(rsz >= len) {
            cBuffer.get(b, offset, len);
            return len;
        } else {
            cBuffer.get(b, offset,rsz);
            cBuffer = null;
            int subRsz = read(b, offset + rsz, len - rsz);
            return subRsz > 0 ? (rsz + subRsz) : rsz;
        }
    }

    @Override
    public void write(byte[] b, int len) throws IOException {
        throw new NotImplementedException();
    }

    @Override
    public void open() throws IOException {
        EOS = false;
        scmWriter.write(SessionControlMessage.builder()
                .command(SessionCommand.ACK)
                .key(key)
                .build());
    }

    @Override
    public void close() throws IOException {
        scmWriter.write(SessionControlMessage.builder()
                .command(SessionCommand.RESET)
                .key(key)
                .build());

        onClose();
    }

    void setSessionKey(String key) {
        this.key = key;
    }

    @Override
    public void handle(SessionControlMessage scm) throws SessionControlException, IOException, IllegalAccessException, InstantiationException, ClassNotFoundException {
        final SessionCommand command = scm.getCommand();
        Object param = converter.resolve(scm.getParam(), command.getParamClass());
        switch (command) {
            case CHUNK:
                SCMChunkParam chunkParam = (SCMChunkParam) param;
                synchronized (dataQueue) {
                    dataQueue.offer(((SCMChunkParam) param).getData());
                    dataQueue.notifyAll();
                    if(chunkParam.getType() == SCMChunkParam.TYPE_LAST) {
                        EOS = true;
                    }
                }
                overallRcvSize += chunkParam.getSizeInBytes();
                break;
            case RESET:
                Log.debug("reset from peer");
                onClose();
                break;
            case ERR:
                SCMErrorParam errorParam = (SCMErrorParam) param;
                throw new SessionControlException(errorParam);
            default:
                sendErrorMessage(key, SCMErrorParam.buildUnsupportedOperation(command));
        }
    }

    @Override
    public void start(Reader reader, Writer writer, Converter converter, SessionControlMessageWriter.Builder builder, Runnable onTeardown) {
        this.onTeardown = onTeardown;
        scmWriter = builder.build(writer);
        this.converter = converter;
    }

    private void onClose() throws IOException {
        if(onTeardown != null) {
            onTeardown.run();
        }
        EOS = true;
        Log.debug("overall rcv size {}", overallRcvSize);
    }

    private void sendErrorMessage(String key, SCMErrorParam errorParam) throws IOException {
        scmWriter.write(SessionControlMessage.builder()
                .key(key)
                .param(errorParam)
                .command(SessionCommand.ERR)
                .build());
    }

}
