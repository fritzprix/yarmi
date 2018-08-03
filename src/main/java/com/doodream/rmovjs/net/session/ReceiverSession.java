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
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;

public class ReceiverSession implements Session, SessionHandler {

    private static final Logger Log = LoggerFactory.getLogger(ReceiverSession.class);
    private String key;
    private InputStream chunkInStream;
    private WritableByteChannel chunkOutChannel;

    private Converter converter;
    private Runnable onTeardown;
    private SessionControlMessageWriter scmWriter;

    ReceiverSession() {
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
                ByteBuffer buffer = ByteBuffer.wrap(chunkParam.getData());
                chunkOutChannel.write(buffer);
                if(chunkParam.getType() == SCMChunkParam.TYPE_LAST) {
                    chunkOutChannel.close();
                }
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
        chunkInStream.close();
    }

    private void sendErrorMessage(String key, SCMErrorParam errorParam) throws IOException {
        scmWriter.write(SessionControlMessage.builder()
                .key(key)
                .param(errorParam)
                .command(SessionCommand.ERR)
                .build());
    }

}
