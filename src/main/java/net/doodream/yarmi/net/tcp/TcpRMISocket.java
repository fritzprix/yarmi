package net.doodream.yarmi.net.tcp;


import net.doodream.yarmi.net.RMISocket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

public class TcpRMISocket implements RMISocket {

    private Socket socket;
    private SocketAddress remoteAddress;
    public TcpRMISocket(Socket client) {
        socket = client;
        remoteAddress = socket.getRemoteSocketAddress();
    }

    public TcpRMISocket(String host, int port) {

        remoteAddress = new InetSocketAddress(host, port);
        socket = null;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        if(socket == null || socket.isClosed()) {
            throw new IllegalStateException("Connection is not opened");
        }
        return socket.getInputStream();
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        if(socket == null || socket.isClosed()) {
            throw new IllegalStateException("Connection is not opened");
        }
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
    public void open() throws IOException {
        InetSocketAddress inetSocketAddress = (InetSocketAddress) remoteAddress;
        socket = new Socket(inetSocketAddress.getHostName(), inetSocketAddress.getPort());
    }

    @Override
    public boolean isConnected() {
        return socket.isConnected();
    }

    @Override
    public boolean isClosed() {
        return socket.isClosed();
    }

    @Override
    public String getRemoteName() {
        return ((InetSocketAddress) remoteAddress).getHostName();
    }


}
