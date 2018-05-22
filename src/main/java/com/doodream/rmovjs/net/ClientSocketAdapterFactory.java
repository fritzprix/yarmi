package com.doodream.rmovjs.net;


import com.doodream.rmovjs.model.RMIServiceInfo;

import java.io.IOException;

public interface ClientSocketAdapterFactory {
    ClientSocketAdapter handshake(RMIServiceInfo serviceInfo, RMISocket clientSocket) throws IOException;
}
