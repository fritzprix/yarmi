package com.doodream.rmovjs.net.session;

import com.doodream.rmovjs.serde.Converter;
import com.doodream.rmovjs.serde.Reader;
import com.doodream.rmovjs.serde.Writer;

import java.io.IOException;

public interface SessionHandler {
    void handle(SessionControlMessage scm) throws SessionControlException, IOException, IllegalAccessException, InstantiationException, ClassNotFoundException;
    void start(Reader reader, Writer writer, Converter converter, SessionControlMessageWriter.Builder builder, Runnable onTeardown);
}

