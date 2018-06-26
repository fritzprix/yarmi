package com.doodream.rmovjs.net.session;

import com.doodream.rmovjs.serde.RMIReader;
import com.doodream.rmovjs.serde.RMIWriter;

import java.io.IOException;

public interface SessionHandler {
    void handle(SessionControlMessage scm) throws IllegalStateException, IOException;
    void start(RMIReader reader, RMIWriter writer, SessionControlMessageWriter.Builder builder, Runnable onTeardown);
}

