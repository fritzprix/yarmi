package net.doodream.yarmi.sdp;

import net.doodream.yarmi.server.RMIService;

import java.io.IOException;

public interface ServiceRegistry {

    /**
     * start to handle discovery queries from clients
     * @throws IllegalStateException
     * @throws IOException
     */
    void start() throws IllegalStateException, IOException;

    /**
     * stop to handle discovery queries
     * @throws IllegalStateException
     */
    void stop() throws IllegalStateException;

    /**
     * register service into service registry and allow client discovery service
     * @param service
     * @return unique id
     * @throws IllegalArgumentException
     */
    int register(RMIService service) throws IllegalArgumentException;

    /**
     * unregister service from service registry
     * @param id unique id which is returned {@link #register(RMIService)}
     * @throws IllegalArgumentException
     */
    void unregister(int id) throws IllegalArgumentException;
}
