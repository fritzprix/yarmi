package com.doodream.rmovjs.sdp;

import java.io.IOException;

public interface ServiceDiscovery {

    /**
     * initiate single service discovery trial
     * @param service class declaration of the RMI service which is annotated with {@link com.doodream.rmovjs.annotation.server.Service}
     * @param listener listener to receive discovery result asynchronously
     * @throws IOException I/O related error occurred during discovery process
     * @throws IllegalArgumentException invalid parameter value
     */
    void start(Class service, ServiceDiscoveryListener listener) throws IOException, IllegalArgumentException;
    void stop();

}
