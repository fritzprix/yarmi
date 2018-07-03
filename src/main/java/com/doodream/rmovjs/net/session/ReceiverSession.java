package com.doodream.rmovjs.net.session;

import com.doodream.rmovjs.net.session.param.SCMChunkParam;
import com.doodream.rmovjs.net.session.param.SCMErrorParam;
import com.doodream.rmovjs.serde.Reader;
import com.doodream.rmovjs.serde.Writer;
import com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.Optional;

public class ReceiverSession implements Session, SessionHandler {

    private static final Logger Log = LoggerFactory.getLogger(ReceiverSession.class);
    private String key;
    private InputStream chunkInStream;
    private WritableByteChannel chunkOutChannel;

    private final ByteBuffer readBuffer;
    private Runnable onTeardown;
    private Reader reader;
    private SessionControlMessageWriter scmWriter;

    ReceiverSession() {

        readBuffer = ByteBuffer.allocate(BlobSession.CHUNK_MAX_SIZE_IN_BYTE);
    }

    @Override
    public int read(byte[] b, int offset, int len) throws IOException {
        return chunkInStream.read(b, offset, len);
    }

    @Override
    public void write(byte[] b, int len) throws IOException {
        throw new NotImplementedException();
    }

    @Override
    public void open() throws IOException {
        final PipedInputStream pipedInputStream = new PipedInputStream();
        chunkInStream = new BufferedInputStream(pipedInputStream, 64 * 1024);
        chunkOutChannel = Channels.newChannel(new PipedOutputStream(pipedInputStream));

        scmWriter.write(SessionControlMessage.builder()
                .command(SessionCommand.ACK)
                .key(key)
                .param(null)
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
    public Optional<SessionControlMessage> handle(SessionControlMessage scm) throws SessionControlException, IOException {
        final SessionCommand command = scm.getCommand();
        switch (command) {
            case CHUNK:
                SCMChunkParam chunkParam = (SCMChunkParam) scm.getParam();
                Log.trace("Chunk Received : {}", chunkParam);
                final int chunkSize = chunkParam.getSizeInChar();
                byte[] b = new byte[chunkSize * Character.SIZE - Byte.SIZE];
                ByteBuffer buffer = ByteBuffer.wrap(b);
                int rsz = reader.readBlob(buffer);
                String eoc = new String(b, chunkSize - 2, 2);
                Preconditions.checkArgument(BlobSession.CHUNK_DELIMITER.equals(eoc));
                chunkOutChannel.write(readBuffer);
                readBuffer.position(0);
                break;
            case RESET:
                Log.debug("reset from peer");
                onClose();
                break;
            case ERR:
                SCMErrorParam errorParam = (SCMErrorParam) scm.getParam();
                throw new SessionControlException(errorParam);
            default:
                sendErrorMessage(key, SCMErrorParam.buildUnsupportedOperation(command));
        }
        return Optional.empty();
    }

    @Override
    public void start(Reader reader, Writer writer, SessionControlMessageWriter.Builder builder, Runnable onTeardown) {
        this.reader = reader;
        this.onTeardown = onTeardown;
        scmWriter = builder.build(writer);
    }

    private void onClose() throws IOException {
        if(onTeardown != null) {
            onTeardown.run();
        }
        chunkInStream.close();
    }

    private void sendErrorMessage(String key, SCMErrorParam errorParam) throws IOException {
        scmWriter.write(SessionControlMessage.builder()
                .key(key)
                .command(SessionCommand.ERR)
                .param(errorParam)
                .build());
    }

}
