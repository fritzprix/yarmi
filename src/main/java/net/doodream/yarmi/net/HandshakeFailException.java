package net.doodream.yarmi.net;

public class HandshakeFailException extends RuntimeException {
    public HandshakeFailException(ClientSocketAdapter adapter) {
        super(String.format("Handshake failed : %s", adapter.who()));
    }

    public HandshakeFailException() {
        super("Handshake failed");
    }
}
