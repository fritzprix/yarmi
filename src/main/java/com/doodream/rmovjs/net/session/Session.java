package com.doodream.rmovjs.net.session;

import java.io.IOException;

public interface Session {

    /**
     *
     * @param data
     * @param offset
     * @param len
     * @return
     * @throws IOException
     */
    int read(byte[] data, int offset, int len) throws IOException;

    /**
     *
     * @param data
     * @param len
     * @throws IOException
     */
    void write(byte[] data, int len) throws IOException;

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
