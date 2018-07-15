package com.doodream.rmovjs.net.session;

import com.doodream.rmovjs.serde.Writer;

import java.io.IOException;

public interface SessionControlMessageWriter {

    interface Builder {
        SessionControlMessageWriter build(Writer writer);
    }

    /**
     * write simple session control message
     * @param controlMessage
     * @throws IOException
     */
    void write(SessionControlMessage controlMessage) throws IOException;
}
