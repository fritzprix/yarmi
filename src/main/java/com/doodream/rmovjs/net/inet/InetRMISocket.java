package com.doodream.rmovjs.net.inet;


import com.doodream.rmovjs.net.RMISocket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class InetRMISocket implements RMISocket {

    private Socket socket;
    public InetRMISocket(Socket client) {
        socket = client;
    }

    public InetRMISocket(String host, int port) throws IOException {
        socket = new Socket(host, port);
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return socket.getInputStream();
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        return socket.getOutputStream();
    }

    @Override
    public void close() throws IOException {
        if(socket.isClosed()) {
            return;
        }
        socket.close();
    }

    @Override
    public boolean isConnected() {
        return socket.isConnected();
    }


}
