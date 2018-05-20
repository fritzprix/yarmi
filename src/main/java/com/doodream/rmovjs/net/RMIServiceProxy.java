package com.doodream.rmovjs.net;

import com.doodream.rmovjs.example.UserIDPController;
import com.doodream.rmovjs.model.Endpoint;
import com.doodream.rmovjs.model.RMIError;
import com.doodream.rmovjs.model.Request;
import com.doodream.rmovjs.model.Response;

import java.io.IOException;

public interface RMIServiceProxy {
    public static final RMIServiceProxy NULL_PROXY = new RMIServiceProxy() {
        @Override
        public void open() throws IOException {

        }

        @Override
        public Response request(Endpoint endpoint) throws IOException {
            return RMIError.NOT_FOUND.getResponse(Request.builder().build());
        }

        @Override
        public void close() throws IOException {

        }

        @Override
        public boolean provide(Class<UserIDPController> userIDPControllerClass) {
            return false;
        }
    };

    void open() throws IOException;
    Response request(Endpoint endpoint) throws IOException;
    void close() throws IOException;
    boolean provide(Class<UserIDPController> userIDPControllerClass);
}
