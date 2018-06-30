package com.doodream.rmovjs.serde.json;

import com.doodream.rmovjs.serde.Converter;
import com.doodream.rmovjs.serde.Reader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import sun.nio.ch.ChannelInputStream;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;

public class JsonReader implements Reader {
    private static final Logger Log = LogManager.getLogger(JsonReader.class);
    private ReadableByteChannel mChannelIn;
    private Converter mConverter;
    private BufferedReader mReader;

    JsonReader(Converter converter, InputStream is) {
        mChannelIn = Channels.newChannel(is);
        mReader = new BufferedReader(new InputStreamReader(new ChannelInputStream(mChannelIn)));
        mConverter = converter;
    }


    @Override
    public synchronized  <T> T read(Class<T> cls) throws IOException {
        String line = mReader.readLine();
        if(line == null) {
            return null;
        }
        line = line.trim();
        return mConverter.invert(StandardCharsets.UTF_8.encode(line).array(), cls);
    }

    @Override
    public synchronized  <T> T read(Class<T> rawClass, Class<?> parameter) throws IOException {
        String line = mReader.readLine();
        if(line == null) {
            return null;
        }
        line = line.trim();
        return mConverter.invert(StandardCharsets.UTF_8.encode(line).array(), rawClass, parameter);
    }

    @Override
    public synchronized int readBlob(ByteBuffer buffer) throws IOException {
        mChannelIn.read(buffer);
        final int size = buffer.position();
        long skipped = mReader.skip(size >> 1);
        Log.trace("read blob({} bytes) and reader is skipped {} chars", size, skipped);
        return size;
    }
}
