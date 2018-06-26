package com.doodream.rmovjs.serde;

import com.doodream.rmovjs.model.Response;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;

public class RMIWriter {
    private Converter converter;
    private Writer writer;

    public RMIWriter(Converter converter, Writer writer) {
        this.converter = converter;
        this.writer = writer;
    }

    public synchronized void write(Object src) throws IOException {
        converter.write(src, writer);
    }

    public synchronized void writeWithBlob(Object src, InputStream data) throws IOException {
        converter.write(src, writer);
    }

    public synchronized void writeWithBlob(Object src, ByteBuffer data) throws IOException {
        WritableByteChannel channel = Channels.newChannel();
    }
}
