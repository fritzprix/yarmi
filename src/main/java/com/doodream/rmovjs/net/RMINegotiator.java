package com.doodream.rmovjs.net;

import com.doodream.rmovjs.model.RMIServiceInfo;

public interface RMINegotiator {

    /**
     * handle handshake between server and client considering the situation when the security level of transport layer should be updated.
     *
     *
     * @param socket plain text RMISocket
     * @param service ServiceInfo from server
     * @param isClient boolean value indicates whether the handshake is from client side or not
     * @return opened socket
     * @throws HandshakeFailException
     */
    RMISocket handshake(RMISocket socket, RMIServiceInfo service, boolean isClient) throws HandshakeFailException;
}
