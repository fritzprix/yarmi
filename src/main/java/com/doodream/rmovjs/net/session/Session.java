package com.doodream.rmovjs.net.session;

import java.io.IOException;

public interface Session {

    int read(byte[] b, int offset, int len) throws IOException;
    void write(byte[] b, int len) throws IOException;

    /**
     * called by receiver
     * @throws IOException
     */
    void open() throws IOException;

    /**
     * close session from the sender side
     * @throws IOException
     */
    void close() throws IOException;
}
