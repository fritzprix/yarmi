package com.doodream.rmovjs.net.session;

import com.doodream.rmovjs.net.session.param.SCMChunkParam;
import com.doodream.rmovjs.net.session.param.SCMErrorParam;
import com.doodream.rmovjs.serde.Reader;
import com.doodream.rmovjs.serde.Writer;
import com.doodream.rmovjs.serde.json.JsonConverter;
import com.google.common.base.Preconditions;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.internal.LinkedTreeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;

public class ReceiverSession implements Session, SessionHandler {

    private static final Logger Log = LoggerFactory.getLogger(ReceiverSession.class);
    private String key;
    private InputStream chunkInStream;
    private WritableByteChannel chunkOutChannel;

    private Runnable onTeardown;
    private Reader reader;
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
                .build(), null);
    }

    @Override
    public void close() throws IOException {
        scmWriter.write(SessionControlMessage.builder()
                .command(SessionCommand.RESET)
                .key(key)
                .build(), null);

        onClose();
    }

    void setSessionKey(String key) {
        this.key = key;
    }

    @Override
    public Optional<SessionControlMessage> handle(SessionControlMessage scm, String parameter) throws SessionControlException, IOException {
        final SessionCommand command = scm.getCommand();
        switch (command) {
            case CHUNK:
                SCMChunkParam chunkParam = JsonConverter.fromJson(parameter, SCMChunkParam.class);
                Log.debug("chunk {}", chunkParam);
                final int chunkSize = chunkParam.getSizeInChar();
                byte[] b = new byte[chunkSize];
                ByteBuffer buffer = ByteBuffer.wrap(b);
                int rsz = reader.readBlob(buffer);
                String eoc = new String(b, chunkSize - 2, 2);
                Preconditions.checkArgument(Arrays.equals(BlobSession.CHUNK_DELIMITER, eoc.getBytes(StandardCharsets.UTF_8)));
                Log.debug("read size : {} / buffer pos. : {}",rsz,  buffer.position());
                buffer.flip();
                Log.debug(StandardCharsets.UTF_8.decode(buffer).toString());
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
                SCMErrorParam errorParam = JsonConverter.fromJson(parameter, SCMErrorParam.class);
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
                .build(), errorParam);
    }

}
