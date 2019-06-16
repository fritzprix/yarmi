package net.doodream.yarmi.net;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.doodream.yarmi.model.RMIServiceInfo;
import net.doodream.yarmi.serde.Converter;
import net.doodream.yarmi.serde.Reader;

public interface Negotiator {

    static Negotiator getDefault(RMIServiceInfo info, RMISocket socket) {
        return new DefaultNegotiator();
    }

    /**
     * handle handshake between server and client considering the situation when the
     * security level of transport layer should be updated.
     *
     *
     * @param socket   plain text RMISocket
     * @param service  ServiceInfo from server
     * @param isClient boolean value indicates whether the handshake is from client
     *                 side or not
     * @return opened socket
     * @throws HandshakeFailException
     */
    RMISocket handshake(RMISocket socket, RMIServiceInfo service, Converter converter, boolean isClient)
            throws HandshakeFailException;
}
