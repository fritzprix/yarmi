package com.doodream.rmovjs.client;

import com.doodream.rmovjs.annotation.RMIException;
import com.doodream.rmovjs.annotation.server.Controller;
import com.doodream.rmovjs.annotation.server.Service;
import com.doodream.rmovjs.method.RMIMethod;
import com.doodream.rmovjs.model.RMIError;
import com.doodream.rmovjs.model.RMIServiceInfo;
import com.doodream.rmovjs.net.ServiceProxy;
import com.doodream.rmovjs.sdp.ServiceDiscovery;
import com.doodream.rmovjs.sdp.ServiceDiscoveryListener;
import com.google.common.base.Preconditions;
import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 *  High-Available RMI Client
 */
public class HaRMIClient<T> implements InvocationHandler {

    private interface Selectable {
        RMIClient selectNext(List<RMIClient> proxies, RMIClient lastSelected);
    }

    /**
     * types of policy used to select service every request
     */
    public enum RequestRoutePolicy implements Selectable {
        RoundRobin {
            @Override
            public RMIClient selectNext(List<RMIClient> proxies, RMIClient lastSelected) {
                if(lastSelected == null) {
                    return proxies.get(0);
                }
                if(proxies.contains(lastSelected)) {
                    int idx =  proxies.indexOf(lastSelected) + 1;
                    if(idx >= proxies.size()) {
                        idx = 0;
                    }
                    return proxies.get(idx);
                }
                return proxies.get(0);
            }
        },
        FastestFirst {
            @Override
            public RMIClient selectNext(List<RMIClient> proxies, RMIClient lastSelected) {
                return Observable.fromIterable(proxies)
                        .sorted(RMIClient::compareTo)
                        .blockingFirst();
            }
        },
        Random {
            @Override
            public RMIClient selectNext(List<RMIClient> proxies, RMIClient lastSelected) {
                return null;
            }
        }
    }

    public interface AvailabilityChangeListener {
        void onAvailabilityChanged(int availableServices);
    }

    private static final long DEFAULT_QOS_UPDATE_PERIOD = 2000L;
    private static final long AVAILABILITY_WAIT_TIMEOUT = 5000L;
    private static final int MAX_TRIAL_COUNT = 3;
    private static final Logger Log = LoggerFactory.getLogger(HaRMIClient.class);

    private AvailabilityChangeListener availabilityChangeListener;
    private RequestRoutePolicy routePolicy;
    private CompositeDisposable compositeDisposable;
    private RMIClient lastProxy;
    private long qosFactor;
    private ExecutorService listenerInvoker;
    private HashSet<String> discoveredProxySet;
    private final ArrayList<RMIClient> clients;
    private Class<T> controller;
    private Class svc;
    private long qosUpdateTime;
    private TimeUnit qosUpdateTimeUnit;

    /**
     * close call proxy and release its resources
     * @param callProxy call proxy returned by {@link #create(ServiceDiscovery, long, Class, Class, RequestRoutePolicy, AvailabilityChangeListener)}
     * @param force if false, caller wait until the on-going requests are finished. if true, close the network connection immediately without waiting
     */
    public static void destroy(Object callProxy, boolean force) {
        final HaRMIClient client = (HaRMIClient) Proxy.getInvocationHandler(callProxy);
        if(client == null) {
            return;
        }
        client.close(force);
    }

    /**
     * create call proxy for multiple services
     * it tries to keep connection to services as many as possible
     * @param discovery @{@link ServiceDiscovery} used to discover service
     * @param qos latency in millisecond which service should meet to keep connection to this client
     * @param svc service definition class annotated by {@link Service}
     * @param ctrl controller interface
     * @param policy policy used to select service
     * @param listener listener to monitor the change of service availability
     * @param <T> controller type which proxy is created from
     * @return call proxy
     */
    public static <T> T create(ServiceDiscovery discovery, long qos, Class svc, Class<T> ctrl, RequestRoutePolicy policy, AvailabilityChangeListener listener) {

        Service service = (Service) svc.getAnnotation(Service.class);
        Preconditions.checkNotNull(service);


        Controller controller = Observable.fromArray(svc.getDeclaredFields())
                .filter(field -> field.getType().equals(ctrl))
                .map(field -> field.getAnnotation(Controller.class))
                .blockingFirst(null);


        List<Method> validMethods = Observable.fromArray(ctrl.getMethods())
                .filter(RMIMethod::isValidMethod).toList().blockingGet();

        final RMIServiceInfo serviceInfo = RMIServiceInfo.from(svc);
        Preconditions.checkNotNull(serviceInfo, "Invalid Service Class %s", svc);

        Preconditions.checkArgument(validMethods.size() > 0);
        Preconditions.checkNotNull(controller, "no matched controller");
        Preconditions.checkArgument(ctrl.isInterface());

        HaRMIClient<T> haRMIClient = new HaRMIClient<>(svc, ctrl, qos, DEFAULT_QOS_UPDATE_PERIOD, TimeUnit.MILLISECONDS, policy);
        haRMIClient.availabilityChangeListener = listener;


        CompositeDisposable compositeDisposable = new CompositeDisposable();
        compositeDisposable.add(startDiscovery(discovery, svc)
                .subscribeOn(Schedulers.newThread())
                .subscribe(haRMIClient::registerProxy, haRMIClient::onError));

        haRMIClient.setDisposable(compositeDisposable);

        return (T) Proxy.newProxyInstance(ctrl.getClassLoader(),new Class[]{ ctrl }, haRMIClient);
    }

    /**
     * check the availability of the service
     * @param callProxy call proxy returned by {@link #create(ServiceDiscovery, long, Class, Class, RequestRoutePolicy, AvailabilityChangeListener)}
     * @param blockUntilAvailable if true, caller will block until at least a service available, otherwise return immediately
     * @return true, if there is at least an available service, otherwise false
     */
    public static boolean isAvailable(Object callProxy, boolean blockUntilAvailable) {
        HaRMIClient haRMIClient = (HaRMIClient) Proxy.getInvocationHandler(callProxy);
        if(haRMIClient == null) {
            return false;
        }
        int availability;
        synchronized (haRMIClient.clients) {
            while (!((availability = haRMIClient.getUpdatedAvailability()) > 0) && blockUntilAvailable) {
                try {
                    haRMIClient.clients.wait(AVAILABILITY_WAIT_TIMEOUT);
                } catch (InterruptedException e) {
                    return false;
                }
            }
        }
        return availability > 0;
    }

    private synchronized int getUpdatedAvailability() {
        return clients.size();
    }

    /**
     * private constructor
     * @param svc service definition annotated with {@link Service}
     * @param ctrl class of controller interface
     * @param qos minimum Quality of Service (latency) used to determine the service is good or bad, if the service will be disconnected if it doesn't satisfy the QoS
     * @param qosUpdatePeriod time value how frequently the QoS be measured.
     * @param timeUnit time unit for qosUpdatePeriod
     * @param policy policy used to select service for each request
     */
    private HaRMIClient(Class svc, Class<T> ctrl, long qos, long qosUpdatePeriod, TimeUnit timeUnit, RequestRoutePolicy policy) {
        this.svc = svc;
        this.controller = ctrl;
        this.routePolicy = policy;
        this.qosFactor = qos;
        listenerInvoker = Executors.newSingleThreadExecutor();
        clients = new ArrayList<>();
        discoveredProxySet = new HashSet<>();
        qosUpdateTime = qosUpdatePeriod;
        qosUpdateTimeUnit = timeUnit;
    }

    private void onError(Throwable throwable) {
        Log.error(throwable.getLocalizedMessage());
        close(true);
    }


    private void setDisposable(CompositeDisposable compositeDisposable) {
        this.compositeDisposable = compositeDisposable;
    }

    private synchronized void registerProxy(ServiceProxy serviceProxy) {
        if (serviceProxy == null) {
            return;
        }
        if (discoveredProxySet.add(serviceProxy.who())) {
            clients.add(RMIClient.createClient(serviceProxy, svc, controller, qosFactor, qosUpdateTime, qosUpdateTimeUnit));
        }

        listenerInvoker.submit(() -> {
            synchronized (clients) {
                availabilityChangeListener.onAvailabilityChanged(clients.size());
                clients.notifyAll();
            }
        });
    }


    private static Observable<ServiceProxy> startDiscovery(ServiceDiscovery discovery, Class svc) {
        return Observable.create(emitter -> discovery.startDiscovery(svc, false, new ServiceDiscoveryListener() {

            @Override
            public void onDiscovered(RMIServiceInfo info) {
                emitter.onNext(RMIServiceInfo.toServiceProxy(info));
            }

            @Override
            public void onDiscoveryStarted() {

            }

            @Override
            public void onDiscoveryFinished() {
                emitter.onComplete();
            }
        }));
    }


    private void close(boolean force) {
        compositeDisposable.dispose();
        listenerInvoker.shutdown();
        clients.forEach(client -> {
            try {
                client.close(force);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }


    private RMIClient selectNext() throws TimeoutException {
        RMIClient selected;
        int trial = MAX_TRIAL_COUNT;
        try {
            do {
                selected = routePolicy.selectNext(clients, lastProxy);
                if (selected != null) {
                    return selected;
                }
                synchronized (clients) {
                    clients.wait();
                }
            } while (trial-- > 0);
        } catch (InterruptedException ignore) { }
        throw new TimeoutException(String.format("fail to select next client /w %s", routePolicy));
    }


    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        lastProxy = selectNext();
        Preconditions.checkNotNull(lastProxy, "invalid call proxy : null call proxy");
        try {
            return lastProxy.invoke(lastProxy, method, args);
        } catch (RMIException e) {
            if(RMIError.isServiceBad(e.code())) {
                purgeBadProxy(lastProxy);
            }
            // rethrow it
            throw e;
        }
    }

    @Override
    protected void finalize() throws Throwable {
        close(true);
        super.finalize();
    }

    /**
     * purge bad proxy
     * @param client client which doesn't meet QoS requirement
     */
    private synchronized void purgeBadProxy(RMIClient client) {
        Log.debug("Purge client {}", client);
        clients.remove(client);
        if(!discoveredProxySet.remove(client.who())) {
            Log.warn("client ({}) is not in the discovered set", client);
        }
        try {
            client.close(true);
        } catch (IOException e) {
            Log.warn(e.getMessage());
        }
        listenerInvoker.submit(() -> {
            synchronized (client) {
                availabilityChangeListener.onAvailabilityChanged(clients.size());
                client.notifyAll();
            }
        });
    }

}
