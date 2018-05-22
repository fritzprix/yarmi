package com.doodream.rmovjs.net.inet;


import com.doodream.rmovjs.model.RMIServiceInfo;
import com.doodream.rmovjs.net.ClientSocketAdapter;
import com.doodream.rmovjs.net.ClientSocketAdapterFactory;
import com.doodream.rmovjs.net.HandshakeFailException;
import com.doodream.rmovjs.net.RMISocket;

import java.io.IOException;

public class InetClientSocketAdapterFactory implements ClientSocketAdapterFactory {

    @Override
    public ClientSocketAdapter handshake(RMIServiceInfo serviceInfo, RMISocket clientSocket) throws IOException {
        InetClientSocketAdapter clientAdapter = new InetClientSocketAdapter(clientSocket);
        if(clientAdapter.handshake(serviceInfo)) {
            return clientAdapter;
        }
        throw new HandshakeFailException(clientAdapter);
    }
}
