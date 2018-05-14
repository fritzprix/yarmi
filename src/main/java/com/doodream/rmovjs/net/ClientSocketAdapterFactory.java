package com.doodream.rmovjs.net;


import com.doodream.rmovjs.model.RMIServiceInfo;
import io.reactivex.Observable;

import java.io.IOException;

public interface ClientSocketAdapterFactory {
    Observable<ClientSocketAdapter> handshake(RMIServiceInfo serviceInfo, RMISocket clientSocket) throws IOException;
}
