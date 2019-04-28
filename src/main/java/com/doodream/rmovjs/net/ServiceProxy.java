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
            public boolean open() {
                // NO OP
                return false;
            }


            @Override
            public Response request(Endpoint endpoint, long timeoutInMilliSec, Object ...args) {
                return Response.from(RMIError.NOT_FOUND);
            }

            @Override
            public void close(boolean force) {
                // NO OP
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
        boolean open() throws IOException, IllegalAccessException, InstantiationException;
        Response request(Endpoint endpoint, long timeoutMilliSec, Object ...args) throws IOException;
        void close(boolean force) throws IOException;
        String who();
        boolean provide(Class controller);
}
