package net.doodream.yarmi.net;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface RMISocket {
    InputStream getInputStream() throws IOException;
    OutputStream getOutputStream() throws IOException;
    void close() throws IOException;
    void open() throws IOException;
    boolean isConnected();
    boolean isClosed();
    String getRemoteName();
}
