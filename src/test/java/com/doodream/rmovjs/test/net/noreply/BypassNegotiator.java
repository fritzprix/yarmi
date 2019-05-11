package com.doodream.rmovjs.test.net.noreply;

import com.doodream.rmovjs.model.RMIServiceInfo;
import com.doodream.rmovjs.net.HandshakeFailException;
import com.doodream.rmovjs.net.Negotiator;
import com.doodream.rmovjs.net.RMISocket;
import com.doodream.rmovjs.serde.Converter;

public class BypassNegotiator implements Negotiator {
    @Override
    public RMISocket handshake(RMISocket socket, RMIServiceInfo service, Converter converter, boolean isClient) throws HandshakeFailException {
        return socket;
    }
}
