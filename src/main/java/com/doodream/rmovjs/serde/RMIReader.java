package com.doodream.rmovjs.serde;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

/**
 * considering concurrent RMI call scenario, reader operation (like read) should be atomic
 */
public class RMIReader {
    private Converter converter;
    private Reader reader;
    private ReadableByteChannel inChannel;

    public RMIReader(Converter converter, InputStream is) {
        this.inChannel = Channels.newChannel(is);
        this.reader = converter._reader(is);
        this.converter = converter;
    }

    public synchronized <T> T read(Class<T> cls) throws IOException {
        return converter.read(reader, cls);
    }

    public synchronized <T> T read(Reader reader, Class<T> rawClass, Class<?> parameter) throws IOException {
        return converter.read(reader, rawClass, parameter);
    }

    public int readBlob(ByteBuffer buffer, int size) throws IOException {
        return reader.read(c, 0 , length);
    }
}
