package com.doodream.rmovjs.net;

import com.doodream.rmovjs.model.RMIServiceInfo;
import io.reactivex.Observable;

public interface ServerSocketAdapterFactory {

    Observable<ServerSocketAdapter> handshake(RMIServiceInfo serviceInfo, RMISocket socket);
}
