package com.doodream.rmovjs.serde.json;

import com.doodream.rmovjs.serde.Converter;
import com.doodream.rmovjs.serde.Writer;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;

public class JsonWriter implements Writer {

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
}
