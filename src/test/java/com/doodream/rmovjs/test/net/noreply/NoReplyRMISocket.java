package com.doodream.rmovjs.test.net.noreply;

import com.doodream.rmovjs.net.RMISocket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class NoReplyRMISocket implements RMISocket {
    private volatile boolean isConnected;
    private volatile boolean isOpened;
    @Override
    public InputStream getInputStream() throws IOException {
        return new InputStream() {
            @Override
            public int read() throws IOException {
                synchronized (NoReplyRMISocket.this) {
                    try {
                        NoReplyRMISocket.this.wait(10000L);
                    } catch (InterruptedException ignored) { }
                }
                return 0;
            }
        };
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        return new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                synchronized (NoReplyRMISocket.this) {
                    try {
                        NoReplyRMISocket.this.wait(10000L);
                    } catch (InterruptedException ignored) { }
                }
            }
        };
    }

    @Override
    public void close() throws IOException {
        synchronized (this) {
            isOpened = false;
            isConnected = false;
            notifyAll();
        }
    }

    @Override
    public void open() throws IOException {
        isOpened = true;
        isConnected = true;
    }

    @Override
    public boolean isConnected() {
        return isConnected;
    }

    @Override
    public boolean isClosed() {
        return !isOpened;
    }

    @Override
    public String getRemoteName() {
        return "no-reply";
    }
}
