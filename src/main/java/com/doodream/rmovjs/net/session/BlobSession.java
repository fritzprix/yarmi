package com.doodream.rmovjs.net.session;

import com.doodream.rmovjs.net.session.param.SCMChunkParam;
import com.doodream.rmovjs.serde.Converter;
import com.doodream.rmovjs.serde.GsonConverter;
import com.google.gson.annotations.SerializedName;
import io.reactivex.Observable;
import lombok.Data;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Random;

@Data
public class BlobSession {
    private static final Logger Log = LogManager.getLogger(BlobSession.class);
    private static int GLOBAL_KEY = 0;
    private static String DEFAULT_TYPE = "application/octet-stream";
    private static final Random RANDOM = new Random();

    @SerializedName("key")
    private String key;
    @SerializedName("mime")
    private String mime;
    private transient Runnable onReady;
    private transient Runnable onTeardown;
    private transient Reader reader;
    private transient Writer writer;
    private transient PipedInputStream chunkInStream;
    private transient BufferedOutputStream chunkOutStream;
    private transient SessionControlMessageWriter scmWriter;

    public BlobSession(Runnable onReady) {
        OptionalLong lo = RANDOM.longs(4).reduce((left, right) -> left + right);
        if(!lo.isPresent()) {
            throw new IllegalStateException("fail to generate random");
        }
        key = Integer.toHexString(String.format("%d%d%d",GLOBAL_KEY++, System.currentTimeMillis(), lo.getAsLong()).hashCode());
        mime = DEFAULT_TYPE;
        this.onReady = onReady;
    }

    public BlobSession() {
        this(null);
    }

    public static Optional<BlobSession> findOne(Object[] args) {
        return Observable.fromArray(args).filter(o -> o instanceof BlobSession).cast(BlobSession.class).map(Optional::ofNullable).blockingFirst(Optional.empty());
    }

    public int read(byte[] b, int offset, int len) throws IOException {
        return chunkInStream.read(b, offset, len);
    }

    public void write(byte[] b, int len) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocateDirect(len);
        buffer.position(0);
        buffer.put(b);

        CharBuffer cbuf = buffer.asCharBuffer();
        SCMChunkParam chunkParam = SCMChunkParam.builder()
                .sizeInChar(cbuf.position())
                .build();

        scmWriter.write(SessionControlMessage.builder().command(SessionCommand.CHUNK).param(chunkParam).build());
        writer.write(cbuf.array());
    }

    public void handle(SessionControlMessage scm) throws IllegalStateException, IOException {
        Log.debug("scm : {}" , scm);
        switch (scm.getCommand()) {
            case ACK:
                if(onReady == null) {
                    break;
                }
                onReady.run();
                break;
            case CHUNK:
                SCMChunkParam chunkParam = (SCMChunkParam) scm.getParam();
                char[] cbuf = new char[chunkParam.getSizeInChar()];
                if(!(reader.read(cbuf) > 0)) {

                }
                break;
            case ECHO:
                try {
                    this.scmWriter.write(SessionControlMessage.builder().command(SessionCommand.ECHO_BACK).build());
                } catch (IOException e) {
                    Log.error(e);
                }
                break;
            case ECHO_BACK:

                break;
            case ERR:

                break;
            case RESET:
                if(onTeardown == null) {
                    break;
                }
                onTeardown.run();
                break;
            default:
                throw new IllegalStateException("");
        }
    }

    public void start(Reader reader, Writer writer, Converter converter, SessionControlMessageWriter.Builder builder, Runnable onTeardown) {
        this.reader = reader;
        this.writer = writer;
        this.scmWriter = builder.build(writer, converter);
        this.onTeardown = onTeardown;
    }

    public void open() throws IOException {
        chunkInStream = new PipedInputStream();
        chunkOutStream = new BufferedOutputStream(new PipedOutputStream(chunkInStream));

        scmWriter.write(SessionControlMessage.builder().command(SessionCommand.ACK).key(key).param(null).build());
    }

    public void close() throws IOException {
        // TODO : close blob buffered stream
        scmWriter.write(SessionControlMessage.builder()
                .command(SessionCommand.RESET)
                .key(key)
                .param(null)
                .build());

        onTeardown.run();
    }
}
