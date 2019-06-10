package net.doodream.yarmi.sdp;

import net.doodream.yarmi.Properties;
import net.doodream.yarmi.model.RMIError;
import net.doodream.yarmi.model.RMIServiceInfo;
import net.doodream.yarmi.model.Response;
import net.doodream.yarmi.serde.Converter;
import net.doodream.yarmi.serde.bson.BsonConverter;
import net.doodream.yarmi.server.RMIService;
import net.doodream.yarmi.service.ServiceRegistryController;
import net.doodream.yarmi.service.ServiceRegistryDelegationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class SimpleServiceRegistry implements ServiceRegistry {

    static final int DEFAULT_MULTICAST_TTL = 64;
    static final int DEFAULT_SERVICE_QUERY_PORT = 3041;

    static final String DEFAULT_MULTICAST_GROUP_IP = "224.0.2.118";   // AD-HOC block 1

    private static final Logger Log = LoggerFactory.getLogger(SimpleServiceRegistry.class);
    private static final String DISCOVERY_ADDRESS = Properties.getDiscoveryAddress(DEFAULT_MULTICAST_GROUP_IP);
    private static final int DISCOVERY_PORT = Properties.getDiscoveryPort(DEFAULT_SERVICE_QUERY_PORT);
    private static final int DISCOVERY_TTL = Properties.getDiscoveryTTL(DEFAULT_MULTICAST_TTL);
    private static final int PACKET_SIZE = 64 * 1024;

    private final ExecutorService executorService = Executors.newWorkStealingPool();
    private final AtomicBoolean isRegistryStarted = new AtomicBoolean(false);
    private final AtomicInteger ids = new AtomicInteger(1);
    private final RegistryDelegationService registryDelegationService = new RegistryDelegationService();
    private final ConcurrentHashMap<Integer, RMIServiceInfo> serviceRegistry = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Collection<Integer>> lookupMap = new ConcurrentHashMap<>();
    private final InetAddress networkAdapter;
    private final Converter converter = new BsonConverter();
    private Future<?> registryTask;

    private static class RegistryDelegationService implements ServiceRegistryController {

        private static final Logger Log = LoggerFactory.getLogger(RegistryDelegationService.class);
        private final AtomicInteger ids = new AtomicInteger(1);
        private final HashMap<Integer, RMIServiceInfo> services = new HashMap<>();
        private final HashMap<String, Collection<Integer>> lookupMap = new HashMap<>();
        private final AtomicBoolean isStarted = new AtomicBoolean(false);
        private RMIService delegationService;

        public synchronized void start(InetAddress network) throws IllegalAccessException, IOException, InstantiationException {
            if(isStarted.compareAndSet(false, true)) {
                delegationService = RMIService.create(ServiceRegistryDelegationService.class, this);
                delegationService.listen(network);
            }
        }

        public synchronized void stop() {
            if(isStarted.compareAndSet(true, false)) {
                try {
                    delegationService.stop();
                } catch (IOException e) {
                    Log.warn("fail to stop registry delegation service");
                }
            }
        }

        public synchronized Set<RMIServiceInfo> lookup(SimpleServiceQuery query) {
            final Collection<Integer> ids = lookupMap.get(query.hash);
            final Set<RMIServiceInfo> results = new HashSet<>();
            if (ids != null) {
                for (Integer id : ids) {
                    results.add(services.get(id));
                }
            }
            return results;
        }

        @Override
        public synchronized Response<Integer> register(RMIServiceInfo service) {
            if (service == null) {
                return RMIError.BAD_REQUEST.getResponse();
            }
            final int currentId = ids.getAndIncrement();
            final String hash = String.format("%x", service.hashCode());
            services.put(currentId, service);
            Collection<Integer> ids = lookupMap.get(hash);
            if (ids == null) {
                ids = new HashSet<>();
            }
            ids.add(currentId);
            lookupMap.put(hash, ids);
            return Response.success(currentId);
        }

        @Override
        public synchronized Response unregister(int id) {
            final RMIServiceInfo serviceInfo = services.remove(id);
            if (serviceInfo == null) {
                return RMIError.NOT_FOUND.getResponse();
            } else {
                final String hash = String.format("%x", serviceInfo.hashCode());
                Collection<Integer> ids = lookupMap.get(hash);
                if (ids.remove(id)) {
                    Log.debug("removed lookup table {}", id);
                } else {
                    Log.debug("no entry to remove for {}", id);
                }
            }
            return Response.success(id);
        }
    }

    public SimpleServiceRegistry() throws UnknownHostException {
        this(InetAddress.getLocalHost());
    }

    public SimpleServiceRegistry(InetAddress network) {
        networkAdapter = network;
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        executorService.shutdown();
    }

    @Override
    public synchronized void start() throws IllegalStateException, IOException {
        final InetAddress groupAddress = InetAddress.getByName(DISCOVERY_ADDRESS);
        if (isRegistryStarted.compareAndSet(false, true)) {
            registryTask = executorService.submit(() -> {
                try {
                    //1. start listen multicast channel for discovery request
                    final MulticastSocket mCastSocket = new MulticastSocket(DISCOVERY_PORT);
                    final DatagramPacket packet = new DatagramPacket(new byte[PACKET_SIZE], PACKET_SIZE);
                    mCastSocket.setTimeToLive(DISCOVERY_TTL);
                    mCastSocket.setInterface(networkAdapter);
                    mCastSocket.joinGroup(groupAddress);

                    //2. start service registry service which will be delegated service registry for other applications
                    // which depends on yarmi
                    try {
                        startDelegationService();
                        while (isRegistryStarted.get()) {
                            mCastSocket.receive(packet);
                            byte[] data = packet.getData();
                            if (data == null) {
                                continue;
                            }
                            SimpleServiceQuery query = converter.invert(data, SimpleServiceQuery.class);
                            Collection<RMIServiceInfo> services = lookup(query);
                            if (services != null) {
                                Log.debug("service query hit! {}", services);
                                executorService.submit(() -> {
                                    sendQueryResponse(packet.getAddress(), query, services);
                                    Log.debug("query {} has been responded", query.name);
                                });
                            } else {
                                Log.debug("service query miss!");
                            }
                        }
                    } catch (IOException e) {
                        Log.debug("stop registry service {}", e.getMessage());
                    } finally {
                        stopDelegationService();
                        mCastSocket.leaveGroup(groupAddress);
                        mCastSocket.close();
                    }
                } catch (IOException e) {
                    Log.error("fail to bind multicast socket {}", e.getMessage());
                    // TODO: 19. 6. 2 try to connect another local service registry via RMI
                }
            });

        } else {
            throw new IllegalStateException("already stared!");
        }
    }

    private Collection<RMIServiceInfo> lookup(SimpleServiceQuery query) {
        final Collection<RMIServiceInfo> delegatedServices = registryDelegationService.lookup(query);
        final Collection<Integer> ids = lookupMap.get(query.hash);
        Set<RMIServiceInfo> result = new HashSet<>(delegatedServices);
        if(ids != null) {
            for (Integer id : ids) {
                final RMIServiceInfo service = serviceRegistry.get(id);
                if(service != null) {
                    result.add(service);
                }
            }
        }

        return result;
    }

    private void sendQueryResponse(InetAddress address, SimpleServiceQuery query, Collection<RMIServiceInfo> serviceInfo) {
        // TODO: 19. 6. 2 send query response
        SimpleServiceDiscoveryResponse response = new SimpleServiceDiscoveryResponse();
        response.setQuery(query);
        response.setServices(new HashSet<>(serviceInfo));
        final byte[] data = converter.convert(response);

        executorService.submit(() -> {
            try {
                final DatagramPacket packet = new DatagramPacket(data, data.length, address, query.callbackPort);
                DatagramSocket socket = new DatagramSocket();
                socket.setSoTimeout(1000);
                socket.send(packet);
            } catch (IOException e) {
                Log.warn("fail to send discovery response to {}", address);
            }
        });
    }


    private synchronized void stopDelegationService() {
//        registryDelegationService.stop();
    }


    private synchronized void startDelegationService() {
//        try {
            // start registry delegation service on loopback address
//            registryDelegationService.start(InetAddress.getLoopbackAddress());
//        } catch (IllegalArgumentException | IllegalAccessException | IOException | InstantiationException e) {
//            Log.error("fail to start service registry server {}", e.getMessage(), e);
//        }
    }

    @Override
    public synchronized void stop() throws IllegalStateException {
        if(isRegistryStarted.compareAndSet(true, false)) {
            if(registryTask == null) {
                return;
            }
            if(registryTask.isDone() || registryTask.isCancelled()) {
                return;
            }
            registryTask.cancel(true);
        }
    }

    @Override
    public synchronized int register(RMIService service) throws IllegalArgumentException {
        if(service == null) {
            throw new IllegalArgumentException("null RMI service");
        }
        final RMIServiceInfo serviceInfo = service.getServiceInfo();
        if(serviceInfo == null) {
            throw new IllegalArgumentException("null service info");
        }
        final int currentId = ids.getAndIncrement();
        final String hash = String.format(Locale.ENGLISH, "%x", serviceInfo.hashCode());
        serviceRegistry.put(currentId, serviceInfo);
        Collection<Integer> ids = lookupMap.get(hash);
        if(ids == null) {
            ids = new HashSet<>();
        }
        ids.add(currentId);
        Log.debug("service registered {} {} : {}", currentId, hash, serviceInfo);
        return currentId;
    }

    @Override
    public synchronized void unregister(int serviceId) throws IllegalArgumentException {
        if(serviceId <= 0) {
            throw new IllegalArgumentException("invalid registry id");
        }

        final RMIServiceInfo service = serviceRegistry.get(serviceId);
        if(service == null) {
            Log.debug("no service registry for {}", serviceId);
            return;
        }
        final String hash = String.format(Locale.ENGLISH, "%x", service.hashCode());
        final Collection<Integer> ids = lookupMap.get(hash);
        if(ids == null) {
            Log.debug("no IDs is found for hash : {}", hash);
            return;
        }
        if(ids.remove(serviceId)) {
            Log.debug("service ({}) unregistered", serviceId);
        } else {
            Log.debug("fail to remove service ID {}", serviceId);
        }
    }
}
