package com.doodream.rmovjs.sdp;

import com.doodream.rmovjs.Properties;
import com.doodream.rmovjs.model.RMIServiceInfo;
import com.doodream.rmovjs.serde.bson.BsonConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.*;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class SimpleServiceDiscovery implements ServiceDiscovery {

    private static final Logger Log = LoggerFactory.getLogger(SimpleServiceDiscovery.class);
    private final InetAddress network;
    private final BsonConverter converter = new BsonConverter();

    private final static String DISCOVERY_ADDRESS = Properties.getDiscoveryAddress(SimpleServiceRegistry.DEFAULT_MULTICAST_GROUP_IP);
    private final static int DISCOVERY_PORT = Properties.getDiscoveryPort(SimpleServiceRegistry.DEFAULT_SERVICE_QUERY_PORT);
    private final static int DISCOVERY_TTL = Properties.getDiscoveryTTL(SimpleServiceRegistry.DEFAULT_MULTICAST_TTL);
    private final static long DISCOVERY_TIMEOUT = Properties.getDiscoveryTimeoutInMills();
    private final ExecutorService executorService = Executors.newWorkStealingPool();
    private Future discoveryTask;
    private boolean isDiscovering;

    public SimpleServiceDiscovery(InetAddress address) {
        network = address;
    }

    public SimpleServiceDiscovery() throws UnknownHostException {
        this(InetAddress.getLocalHost());
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        executorService.shutdown();
    }

    @Override
    public void start(Class service, ServiceDiscoveryListener listener) throws IOException, IllegalArgumentException {

        final Set<RMIServiceInfo> services = Collections.newSetFromMap(new ConcurrentHashMap<>());
        final MulticastSocket mCastSocket = new MulticastSocket(DISCOVERY_PORT);
        mCastSocket.setTimeToLive(DISCOVERY_TTL);

        final InetAddress groupAddress = InetAddress.getByName(DISCOVERY_ADDRESS);
        final DatagramSocket uCastSocket = new DatagramSocket();
        mCastSocket.joinGroup(groupAddress);
        uCastSocket.setSoTimeout((int) DISCOVERY_TIMEOUT);

        executorService.submit(() -> {
            synchronized (this) {
                if(isDiscovering) {
                    return;
                }
                isDiscovering = true;
                discoveryTask = executorService.submit(() -> {
                    final int bufferSize = 1024 * 64;
                    final DatagramPacket packet = new DatagramPacket(new byte[bufferSize], bufferSize);

                    try {
                        while(isDiscovering) {
                            uCastSocket.receive(packet);
                            final byte[] data = packet.getData();
                            if(data == null) {
                                continue;
                            }

                            if(data.length == 0) {
                                continue;
                            }
                            SimpleServiceDiscoveryResponse serviceRecord = converter.invert(data, SimpleServiceDiscoveryResponse.class);
                            listener.onServiceDiscovered(serviceRecord.getServices());
                            services.addAll(serviceRecord.getServices());
                        }
                    } catch (IOException e) {
                        stop();
                        Log.debug("stop listening discovery response : {}", e.getMessage());
                    } finally {
                        uCastSocket.close();
                    }
                });
            }


            SimpleServiceQuery serviceQuery = SimpleServiceQuery.from(service, uCastSocket.getLocalPort());
            final byte[] data = converter.convert(serviceQuery);
            final DatagramPacket packet = new DatagramPacket(data, data.length, groupAddress, DISCOVERY_PORT);
            try {
                mCastSocket.send(packet);
            } catch (IOException e) {
                Log.error("fail to send discovery request : {}", e.getMessage());
                stop();
            } finally {
                mCastSocket.close();
            }
        });
    }

    @Override
    public synchronized void stop() {
        isDiscovering = false;
        if(discoveryTask == null) {
            return;
        }
        if(discoveryTask.isCancelled() || discoveryTask.isDone()) {
            return;
        }
        discoveryTask.cancel(true);
    }
}
