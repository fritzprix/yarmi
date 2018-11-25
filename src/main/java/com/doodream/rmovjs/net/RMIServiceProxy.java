package com.doodream.rmovjs.net;

import com.doodream.rmovjs.model.Endpoint;
import com.doodream.rmovjs.model.RMIError;
import com.doodream.rmovjs.model.Response;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public interface RMIServiceProxy {
        RMIServiceProxy NULL_PROXY = new RMIServiceProxy() {
            @Override
            public void open() {
                // NO OP
            }

            @Override
            public boolean isOpen() {
                return false;
            }

            @Override
            public Response request(Endpoint endpoint, long timeoutInMilliSec, Object ...args) {
                return Response.from(RMIError.NOT_FOUND);
            }

            @Override
            public void close() {
                // NO OP
            }

            @Override
            public void startPeriodicQosUpdate(long timeout, long interval, TimeUnit timeUnit) {
                // NO OP
            }

            @Override
            public void stopPeriodicQosUpdate() {
                // NO OP
            }

            @Override
            public Long getQosUpdate(long timeout) {
                return Long.MAX_VALUE;
            }

            @Override
            public Long getQosMeasured() {
                return Long.MAX_VALUE;
            }

            @Override
            public String who() {
                return Integer.toHexString(hashCode());
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
        Response request(Endpoint endpoint, long timeoutMilliSec, Object ...args) throws IOException;
        void close() throws IOException;
        void startPeriodicQosUpdate(long timeout, long interval, TimeUnit timeUnit);
        void stopPeriodicQosUpdate();
        Long getQosUpdate(long timeout);
        Long getQosMeasured();
        String who();
        boolean provide(Class controller);
}
