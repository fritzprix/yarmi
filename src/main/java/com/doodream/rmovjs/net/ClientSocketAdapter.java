package com.doodream.rmovjs.net;


import com.doodream.rmovjs.model.Request;
import com.doodream.rmovjs.model.Response;
import com.doodream.rmovjs.model.ServiceInfo;
import io.reactivex.Observable;

import java.io.IOException;

public interface ClientSocketAdapter {

    void write(Response response) throws IOException;
    Observable<Request> listen() throws IOException;
    void handshake(ServiceInfo serviceInfo) throws HandshakeFailException;
    String who();
    String unique();
}
