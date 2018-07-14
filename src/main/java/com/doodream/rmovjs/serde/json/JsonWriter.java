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
    private boolean lock = false;

    JsonWriter(Converter converter, OutputStream os) {
        mConverter = converter;
        mChannelOut = Channels.newChannel(os);
    }

    private boolean tryLock() {
        try {
            synchronized (this) {
                while (lock) {
                    this.wait();
                }
                lock = true;
            }
            return true;
        } catch (InterruptedException e) {
            Log.error("{}", e);
            return false;
        }
    }

    private void unlock() {
        synchronized (this) {
            lock = false;
            this.notifyAll();
        }
    }

    @Override
    public synchronized void write(Object src) throws IOException {
        Log.debug("write {}", src);
        ByteBuffer json = ByteBuffer.wrap(mConverter.convert(src));
        mChannelOut.write(json);
    }

    @Override
    public synchronized void writeWithBlob(Object src, InputStream data) throws IOException {
        Log.debug("writeWithBlob /w InputStream {}", src);
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
        Log.debug("write size of blob : {}", mChannelOut.write(data));
        data.limit(data.capacity());
    }
}
