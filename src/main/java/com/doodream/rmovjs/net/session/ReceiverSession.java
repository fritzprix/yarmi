package com.doodream.rmovjs.net.session;

import com.doodream.rmovjs.net.session.param.SCMChunkParam;
import com.doodream.rmovjs.net.session.param.SCMErrorParam;
import com.doodream.rmovjs.serde.RMIReader;
import com.doodream.rmovjs.serde.RMIWriter;
import com.google.common.base.Preconditions;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;

public class ReceiverSession implements Session, SessionHandler {

    private String key;
    private InputStream chunkInStream;
    private WritableByteChannel chunkOutChannel;

    private final ByteBuffer readBuffer;
    private Runnable onTeardown;
    private RMIReader reader;
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


    private void readValidChunk(ByteBuffer readBuffer) throws IOException {
        chunkOutChannel.write(readBuffer);
    }

    @Override
    public void open() throws IOException {
        final PipedInputStream pipedInputStream = new PipedInputStream();
        chunkInStream = new BufferedInputStream(pipedInputStream, 64 * 1024);
        chunkOutChannel = Channels.newChannel(new PipedOutputStream(pipedInputStream));

        scmWriter.write(SessionControlMessage.builder()
                .command(SessionCommand.ACK)
                .key(key)
                .param(null).build());
    }

    @Override
    public void close() throws IOException {
        scmWriter.write(SessionControlMessage.builder()
                .command(SessionCommand.RESET)
                .key(key)
                .build());

        onClose();
    }

    public void setSessionKey(String key) {
        this.key = key;
    }

    @Override
    public void handle(SessionControlMessage scm) throws IllegalStateException, IOException {
        final SessionCommand command = scm.getCommand();
        switch (command) {
            case CHUNK:
                SCMChunkParam chunkParam = (SCMChunkParam) scm.getParam();
                final int chunkSize = chunkParam.getSizeInChar();
                char[] cs = new char[chunkSize];
                int rsz = reader.readBlob(readBuffer, chunkSize);
                String eoc = new String(cs, chunkSize - 2, 2);
                Preconditions.checkArgument(BlobSession.CHUNK_DELIMITER.equals(eoc));
                chunkOutChannel.write(readBuffer);
                readBuffer.position(0);
                break;
            case RESET:
                onClose();
                break;
            case ERR:
                throw new RuntimeException("");
            case ACK:
            default:
                sendErrorMessage(key, SCMErrorParam.buildUnsupportedOperation(command));
        }
    }

    @Override
    public void start(RMIReader reader, RMIWriter writer, SessionControlMessageWriter.Builder builder, Runnable onTeardown) {
        this.reader = reader;
        this.onTeardown = onTeardown;
        scmWriter = builder.build(writer);
    }

    private void onClose() throws IOException {
        if(onTeardown != null) {
            onTeardown.run();
        }
        chunkInStream.close();
        chunkOutStream.close();
    }

    private void sendErrorMessage(String key, SCMErrorParam errorParam) throws IOException {
        scmWriter.write(SessionControlMessage.builder()
                .key(key)
                .command(SessionCommand.ERR)
                .param(errorParam)
                .build());
    }

}
