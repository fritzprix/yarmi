package com.doodream.rmovjs.net;

import com.doodream.rmovjs.example.UserIDPController;
import com.doodream.rmovjs.model.Endpoint;
import com.doodream.rmovjs.model.RMIError;
import com.doodream.rmovjs.model.Request;
import com.doodream.rmovjs.model.Response;

import java.io.IOException;

public interface RMIServiceProxy {
    RMIServiceProxy NULL_PROXY = new RMIServiceProxy() {
        @Override
        public void open() {

        }

        @Override
        public Response request(Endpoint endpoint) {
            return RMIError.NOT_FOUND.getResponse(Request.builder().build());
        }

        @Override
        public void close() {

        }

        @Override
        public boolean provide(Class<UserIDPController> userIDPControllerClass) {
            return false;
        }
    };

    void open() throws IOException, IllegalAccessException, InstantiationException;
    Response request(Endpoint endpoint) throws IOException;
    void close() throws IOException;
    boolean provide(Class<UserIDPController> userIDPControllerClass);
}
