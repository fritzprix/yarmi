package com.doodream.rmovjs.net;

import com.doodream.rmovjs.example.template.UserIDPController;
import com.doodream.rmovjs.model.Endpoint;
import com.doodream.rmovjs.model.RMIError;
import com.doodream.rmovjs.model.Response;

import java.io.IOException;

public interface RMIServiceProxy {
        RMIServiceProxy NULL_PROXY = new RMIServiceProxy() {
            @Override
            public void open() {

            }

            @Override
            public boolean isOpen() {
                return false;
            }

            @Override
            public Response request(Endpoint endpoint, Object ...args) {
                return Response.from(RMIError.NOT_FOUND);
            }

            @Override
            public void close() {

            }

            @Override
            public boolean provide(Class controller) {
                return false;
            }
        };

        /**
         * build connection to ServiceAdapter of server
         * can be called
         * @throws IOException
         * @throws IllegalAccessException
         * @throws InstantiationException
         */
        void open() throws IOException, IllegalAccessException, InstantiationException;
        boolean isOpen();
        Response request(Endpoint endpoint, Object ...args) throws IOException;
        void close() throws IOException;
        boolean provide(Class contoller);
}
