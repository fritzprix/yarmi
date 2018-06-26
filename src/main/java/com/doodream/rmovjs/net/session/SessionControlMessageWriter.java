package com.doodream.rmovjs.net.session;

import com.doodream.rmovjs.serde.RMIWriter;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public interface SessionControlMessageWriter {

    interface Builder {
        SessionControlMessageWriter build(RMIWriter writer);
    }

    /**
     * write simple session control message
     * @param controlMessage
     * @throws IOException
     */
    void write(SessionControlMessage controlMessage) throws IOException;

    void writeWithBlob(SessionControlMessage controlMessage, InputStream data) throws IOException;

    void writeWithBlob(SessionControlMessage controlMessage, ByteBuffer buffer) throws IOException;
}
