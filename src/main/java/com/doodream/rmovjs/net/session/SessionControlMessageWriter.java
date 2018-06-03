package com.doodream.rmovjs.net.session;

import java.io.IOException;

public interface SessionControlMessageWriter {
    void write(SessionControlMessage controlMessage) throws IOException;
}
