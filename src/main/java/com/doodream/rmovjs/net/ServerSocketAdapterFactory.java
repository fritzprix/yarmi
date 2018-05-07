package com.doodream.rmovjs.net;

import com.doodream.rmovjs.model.ServiceInfo;
import io.reactivex.Observable;

public interface ServerSocketAdapterFactory {

    Observable<ServerSocketAdapter> handshake(ServiceInfo serviceInfo, RMISocket socket);
}
