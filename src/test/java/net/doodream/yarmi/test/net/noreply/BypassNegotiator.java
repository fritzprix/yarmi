package net.doodream.yarmi.test.net.noreply;

import net.doodream.yarmi.data.RMIServiceInfo;
import net.doodream.yarmi.net.HandshakeFailException;
import net.doodream.yarmi.net.Negotiator;
import net.doodream.yarmi.net.RMISocket;
import net.doodream.yarmi.serde.Converter;

public class BypassNegotiator implements Negotiator {
    @Override
    public RMISocket handshake(RMISocket socket, RMIServiceInfo service, Converter converter, boolean isClient) throws HandshakeFailException {
        return socket;
    }
}
