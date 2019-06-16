package net.doodream.yarmi.sdp;

import net.doodream.yarmi.annotation.server.Service;

import java.io.IOException;

public interface ServiceDiscovery {

    /**
     * initiate single service discovery trial
     * @param service class declaration of the RMI service which is annotated with {@link Service}
     * @param listener listener to receive discovery result asynchronously
     * @throws IOException I/O related error occurred during discovery process
     * @throws IllegalArgumentException invalid parameter value
     */
    void start(Class<?> service, ServiceDiscoveryListener listener) throws IOException, IllegalArgumentException;
    void stop();

}
