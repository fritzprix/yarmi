package com.doodream.rmovjs.net;

import com.doodream.rmovjs.model.Endpoint;
import com.doodream.rmovjs.model.RMIError;
import com.doodream.rmovjs.model.Response;
import com.doodream.rmovjs.net.session.NotImplementedException;
import io.reactivex.disposables.Disposable;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public interface ServiceProxy {
        ServiceProxy NULL_PROXY = new ServiceProxy() {
            private Disposable measurementDisposable;
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
            public String who() {
                return Integer.toHexString(hashCode());
            }

            @Override
            public void startQosMeasurement(long interval, long timeout, TimeUnit timeUnit, QosListener listener) {
                throw new NotImplementedException();
            }


            @Override
            public void stopQosMeasurement(QosListener listener) {
                throw new NotImplementedException();
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
        String who();
        void startQosMeasurement(long interval, long timeout, TimeUnit timeUnit, QosListener listener);
        void stopQosMeasurement(QosListener listener);
        boolean provide(Class controller);
}
