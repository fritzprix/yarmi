package net.doodream.yarmi.net.session;

import net.doodream.yarmi.serde.Converter;
import net.doodream.yarmi.serde.Reader;
import net.doodream.yarmi.serde.Writer;

import java.io.IOException;

public interface SessionHandler {
    void handle(SessionControlMessage scm) throws SessionControlException, IOException, IllegalAccessException, InstantiationException, ClassNotFoundException;
    void start(Reader reader, Writer writer, Converter converter, SessionControlMessageWriter.Builder builder, Runnable onTeardown);
}

