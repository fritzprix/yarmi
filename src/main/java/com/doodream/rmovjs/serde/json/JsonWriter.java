package com.doodream.rmovjs.serde.json;

import com.doodream.rmovjs.serde.Converter;
import com.doodream.rmovjs.serde.Writer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

public class JsonWriter implements Writer {

    private static final Logger Log = LoggerFactory.getLogger(JsonWriter.class);
    private static final int READ_BUFFER_SIZE = 4096;
    private Converter mConverter;
    private WritableByteChannel mChannelOut;

    JsonWriter(Converter converter, OutputStream os) {
        mConverter = converter;
        mChannelOut = Channels.newChannel(os);
    }

    @Override
    public synchronized void write(Object src) throws IOException {
        ByteBuffer json = ByteBuffer.wrap(mConverter.convert(src));
        mChannelOut.write(json);
    }

    @Override
    public synchronized void writeWithBlob(Object src, InputStream data) throws IOException {
        ByteBuffer json = ByteBuffer.wrap(mConverter.convert(src));
        mChannelOut.write(json);

        ReadableByteChannel channelIn = Channels.newChannel(data);
        ByteBuffer buffer = ByteBuffer.allocate(READ_BUFFER_SIZE);

        try {
            while (channelIn.read(buffer) > 0) {
                mChannelOut.write(buffer);
            }
        } catch (IOException ignore) {
        } finally {
            // write residual data into output
            mChannelOut.write(buffer);
        }
    }

    @Override
    public synchronized void writeWithBlob(Object src, ByteBuffer data) throws IOException {
        ByteBuffer json = ByteBuffer.wrap(mConverter.convert(src));
        mChannelOut.write(json);
        data.flip();
        data.limit(data.capacity());
    }
}
