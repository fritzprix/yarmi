package com.doodream.rmovjs.net;


import com.doodream.rmovjs.model.ServiceInfo;
import io.reactivex.Observable;

import java.io.IOException;

public interface ClientSocketAdapterFactory {
    Observable<ClientSocketAdapter> handshake(ServiceInfo serviceInfo, RMISocket clientSocket) throws IOException;
}
