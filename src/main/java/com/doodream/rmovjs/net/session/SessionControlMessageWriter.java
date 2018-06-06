package com.doodream.rmovjs.net.session;

import com.doodream.rmovjs.serde.Converter;

import java.io.IOException;
import java.io.Writer;

public interface SessionControlMessageWriter {
    public interface Builder {
        SessionControlMessageWriter build(Writer writer, Converter converter);
    }
    void write(SessionControlMessage controlMessage) throws IOException;
}
