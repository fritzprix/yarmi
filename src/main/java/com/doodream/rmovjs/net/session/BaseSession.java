package com.doodream.rmovjs.net.session;

import com.doodream.rmovjs.net.session.param.SCMChunkParam;
import com.doodream.rmovjs.net.session.param.SCMErrorParam;
import com.doodream.rmovjs.serde.Converter;
import com.google.common.base.Preconditions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;

public abstract class BaseSession implements Session {
    static final int CHUNK_MAX_SIZE_IN_BYTE = 64 * 1024;
    static final int CHUNK_MAX_SIZE_IN_CHAR = CHUNK_MAX_SIZE_IN_BYTE / Character.SIZE * Byte.SIZE;


    private static String DEFAULT_TYPE = "application/octet-stream";
    private static final Logger Log = LogManager.getLogger(BaseSession.class);
    private static final String CHUNK_DELIMITER_SEQ = "\r\n";


    private final SessionControlMessageWriter scmWriter;
    private final Reader reader;
    private final Writer writer;
    private final Converter converter;
    private final Runnable onTeardown;

    private InputStream chunkInStream;
    private OutputStream chunkOutStream;
    private ByteBuffer writeBuffer;
    private String key;

    public BaseSession(Reader reader, Writer writer, Converter converter, SessionControlMessageWriter.Builder builder, Runnable onTeardown) {
        this.reader = reader;
        this.writer = writer;
        scmWriter = builder.build(writer, converter);
        this.converter = converter;
        this.onTeardown = onTeardown;
        writeBuffer = ByteBuffer.allocate(CHUNK_MAX_SIZE_IN_BYTE);
    }

    public void setSessionkey(String key) {
        this.key = key;
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

        synchronized (writer) {
            scmWriter.write(chunkMessage);
            writer.write(c, 0, len);
        }
        writeBuffer.position(0);
    }

    @Override
    public void write(byte[] b, int len) throws IOException {
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
    public int read(byte[] b, int offset, int len) throws IOException {
        return chunkInStream.read(b, offset, len);
    }

    @Override
    public void open() throws IOException {
        PipedInputStream pis = new PipedInputStream();
        chunkInStream = new BufferedInputStream(pis);
        chunkOutStream = new PipedOutputStream(pis);
    }

    @Override
    public void close() throws IOException {
        chunkOutStream.close();
        chunkInStream.close();
    }

    void handle(SessionControlMessage scm) throws IllegalStateException, IOException {
        final SessionCommand command = scm.getCommand();
        switch(command) {
            case CHUNK:
                SCMChunkParam chunkParam = (SCMChunkParam) scm.getParam();
                final int chunkSize = chunkParam.getSizeInChar();
                final int chunkSeq = chunkParam.getSequence();
                char[] cs = new char[chunkSize];
                int rsz = reader.read(cs);
                String eoc = new String(cs, chunkSize - 2, 2);
                Preconditions.checkArgument(CHUNK_DELIMITER_SEQ.equals(eoc));
                onValidChunk(cs, rsz - 2);
                break;
            case RESET:
                onClose();
                if(onTeardown == null) {
                    break;
                }
                onTeardown.run();
                break;
            case ACK:
                onAcknowledgement();
                break;
            case ERR:
                onError((SCMErrorParam) scm.getParam());
                break;
            default:
                break;
        }

    }

    protected abstract void onError(SCMErrorParam param);

    protected abstract void onAcknowledgement();

    protected abstract void onClose();

    protected abstract void onValidChunk(char[] cs, int i);

}
