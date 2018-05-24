package com.doodream.rmovjs.net.tcp;


import com.doodream.rmovjs.model.Request;
import com.doodream.rmovjs.model.Response;
import com.doodream.rmovjs.net.ClientSocketAdapter;
import com.doodream.rmovjs.net.RMISocket;
import io.reactivex.Observable;

import java.io.IOException;

public class TcpClientSocketAdapter implements ClientSocketAdapter {

    private RMISocket client;

    TcpClientSocketAdapter(RMISocket socket) {
        client = socket;
    }


    @Override
    public void write(Response response) throws IOException {
        response.to(client);
    }

    @Override
    public Observable<Request> listen() throws IOException {
        return Request.from(client);

    }

    @Override
    public String who() {
        return client.getRemoteName();
    }

}
