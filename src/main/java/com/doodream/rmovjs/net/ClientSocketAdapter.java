package com.doodream.rmovjs.net;


import com.doodream.rmovjs.model.Request;
import com.doodream.rmovjs.model.Response;
import com.doodream.rmovjs.model.RMIServiceInfo;
import io.reactivex.Observable;

import java.io.IOException;

public interface ClientSocketAdapter {

    void write(Response response) throws IOException;
    Observable<Request> listen() throws IOException;
    boolean handshake(RMIServiceInfo serviceInfo) throws IOException;
    String who();
    String unique();
}
