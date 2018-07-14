package com.doodream.rmovjs.serde.json;

import com.doodream.rmovjs.serde.Converter;
import com.doodream.rmovjs.serde.Reader;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final Logger Log = LoggerFactory.getLogger(JsonReader.class);
    private ReadableByteChannel mChannelIn;
    private Converter mConverter;
    private InputStream is;

    JsonReader(Converter converter, InputStream is) {
        mChannelIn = Channels.newChannel(is);
        this.is = is;
        mConverter = converter;
    }


    @Override
    public synchronized  <T> T read(Class<T> cls) throws IOException {
        final BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        String line = reader.readLine();
        if(line == null) {
            return null;
        }
        line = line.trim();
        return mConverter.invert(StandardCharsets.UTF_8.encode(line).array(), cls);
    }

    @Override
    public synchronized  <T> T read(Class<T> rawClass, Class<?> parameter) throws IOException {
        final BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        String line = reader.readLine();
        if(line == null) {
            return null;
        }
        line = line.trim();
        return mConverter.invert(StandardCharsets.UTF_8.encode(line).array(), rawClass, parameter);
    }
}
