package com.doodream.rmovjs.net;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface RMISocket {
    InputStream getInputStream() throws IOException;
    OutputStream getOutputStream() throws IOException;
    void close() throws IOException;
    boolean isConnected();
}
