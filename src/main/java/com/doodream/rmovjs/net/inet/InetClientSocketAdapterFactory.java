package com.doodream.rmovjs.net.inet;


import com.doodream.rmovjs.model.ServiceInfo;
import com.doodream.rmovjs.net.ClientSocketAdapter;
import com.doodream.rmovjs.net.ClientSocketAdapterFactory;
import com.doodream.rmovjs.net.RMISocket;
import io.reactivex.Observable;

import java.io.IOException;

public class InetClientSocketAdapterFactory implements ClientSocketAdapterFactory {

    @Override
    public Observable<ClientSocketAdapter> handshake(ServiceInfo serviceInfo, RMISocket clientSocket) throws IOException {
        InetClientSocketAdapter clientAdapter = new InetClientSocketAdapter(clientSocket);

        return Observable.just(clientAdapter)
                .doOnNext(inetClientAdapter -> inetClientAdapter.handshake(serviceInfo))
                .cast(ClientSocketAdapter.class);
    }
}
