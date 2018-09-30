package com.doodream.rmovjs.client;

import com.doodream.rmovjs.annotation.RMIException;
import com.doodream.rmovjs.annotation.server.Controller;
import com.doodream.rmovjs.annotation.server.Service;
import com.doodream.rmovjs.method.RMIMethod;
import com.doodream.rmovjs.model.RMIError;
import com.doodream.rmovjs.model.RMIServiceInfo;
import com.doodream.rmovjs.net.RMIServiceProxy;
import com.doodream.rmovjs.sdp.ServiceDiscovery;
import com.doodream.rmovjs.sdp.ServiceDiscoveryListener;
import com.google.common.base.Preconditions;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;
import io.reactivex.schedulers.Schedulers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Comparator;
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

    /**
     * policy
     */
    public enum RequestRoutePolicy {
        RoundRobin {
            @Override
            public Object selectNext(List<Object> proxies, Object lastSelected) {
               return proxies.get(0);
            }
        },
        FastestFirst {
            @Override
            public Object selectNext(List<Object> proxies, Object lastSelected) {
                return Observable.fromIterable(proxies)
                        .sorted(new Comparator<Object>() {
                            @Override
                            public int compare(Object o1, Object o2) {
                                return RMIClient.access(o1).compareTo(RMIClient.access(o2));
                            }
                        })
                        .blockingFirst();
            }
        };

        public abstract Object selectNext(List<Object> proxies, Object lastSelected);
    }

    public interface AvailabilityChangeListener {
        void onAvailabilityChanged(int availableServices);
    }

    private static final long DEFAULT_QOS_UPDATE_PERIOD = 2000L;
    private static final long AVAILABILITY_WAIT_TIMEOUT = 5000L;
    private static final Logger Log = LoggerFactory.getLogger(HaRMIClient.class);

    private AvailabilityChangeListener availabilityChangeListener;
    private RequestRoutePolicy routePolicy;
    private CompositeDisposable compositeDisposable;
    private Object lastProxy;
    private long qosFactor;
    private ExecutorService listenerInvoker;
    private HashSet<String> discoveredProxySet;
    private final ArrayList<Object> clients;
    private Class<T> controller;
    private Class svc;
    private long qosUpdateTime;
    private TimeUnit qosUpdateTimeUnit;

    public static void destroy(Object callProxy, boolean force) {
        final HaRMIClient client = (HaRMIClient) Proxy.getInvocationHandler(callProxy);
        if(client == null) {
            return;
        }
        client.close(force);
    }

    public static <T> T create(ServiceDiscovery discovery, long qos, Class svc, final Class<T> ctrl, RequestRoutePolicy policy, AvailabilityChangeListener listener) {

        Service service = (Service) svc.getAnnotation(Service.class);
        Preconditions.checkNotNull(service);


        Controller controller = Observable.fromArray(svc.getDeclaredFields())
                .filter(new Predicate<Field>() {
                    @Override
                    public boolean test(Field field) throws Exception {
                        return field.getType().equals(ctrl);
                    }
                })
                .map(new Function<Field, Controller>() {
                    @Override
                    public Controller apply(Field field) throws Exception {
                        return field.getAnnotation(Controller.class);
                    }
                })
                .blockingFirst(null);


        List<Method> validMethods = Observable.fromArray(ctrl.getMethods())
                .filter(new Predicate<Method>() {
                    @Override
                    public boolean test(Method method) throws Exception {
                        return RMIMethod.isValidMethod(method);
                    }
                })
                .toList()
                .blockingGet();

        final RMIServiceInfo serviceInfo = RMIServiceInfo.from(svc);
        Preconditions.checkNotNull(serviceInfo, "Invalid Service Class %s", svc);

        Preconditions.checkArgument(validMethods.size() > 0);
        Preconditions.checkNotNull(controller, "no matched controller");
        Preconditions.checkArgument(ctrl.isInterface());

        final HaRMIClient<T> haRMIClient = new HaRMIClient<>(svc, ctrl, qos, DEFAULT_QOS_UPDATE_PERIOD, TimeUnit.MILLISECONDS, policy);
        haRMIClient.availabilityChangeListener = listener;


        CompositeDisposable compositeDisposable = new CompositeDisposable();
        compositeDisposable.add(startDiscovery(discovery, svc)
                .subscribeOn(Schedulers.newThread())
                .subscribe(new Consumer<RMIServiceProxy>() {
                    @Override
                    public void accept(RMIServiceProxy rmiServiceProxy) throws Exception {
                        haRMIClient.registerProxy(rmiServiceProxy);
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        haRMIClient.onError(throwable);
                    }
                }));

        haRMIClient.setDisposable(compositeDisposable);

        return (T) Proxy.newProxyInstance(ctrl.getClassLoader(),new Class[]{ ctrl }, haRMIClient);
    }

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

    private int getUpdatedAvailability() {
        return clients.size();
    }


    private HaRMIClient(Class svc, Class<T> ctrl, long qos, long qosUpdatePeriod, TimeUnit timeUnit, RequestRoutePolicy policy) {
        this.svc = svc;
        this.controller = ctrl;
        this.routePolicy = policy;
        Log.debug("policy {}", policy);
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

    private synchronized void registerProxy(RMIServiceProxy serviceProxy) {
        if (serviceProxy == null) {
            return;
        }

        if (discoveredProxySet.add(serviceProxy.who())) {
            Log.debug("try to add client");
            clients.add(RMIClient.create(serviceProxy, svc, controller, qosFactor, qosUpdateTime, qosUpdateTimeUnit));
            Log.debug("client is added");
        }

        listenerInvoker.submit(new Runnable() {
            @Override
            public void run() {
                synchronized (clients) {
                    availabilityChangeListener.onAvailabilityChanged(clients.size());
                    clients.notifyAll();
                }
            }
        });
    }


    private static Observable<RMIServiceProxy> startDiscovery(final ServiceDiscovery discovery, final Class svc) {
        return Observable.create(new ObservableOnSubscribe<RMIServiceProxy>() {
            @Override
            public void subscribe(final ObservableEmitter<RMIServiceProxy> emitter) throws Exception {
                discovery.startDiscovery(svc, false, new ServiceDiscoveryListener() {
                    @Override
                    public void onDiscovered(RMIServiceProxy proxy) {
                        emitter.onNext(proxy);
                    }

                    @Override
                    public void onDiscoveryStarted() {

                    }

                    @Override
                    public void onDiscoveryFinished() {
                        emitter.onComplete();
                    }
                });
            }
        });
    }


    private void close(final boolean force) {
        compositeDisposable.dispose();
        listenerInvoker.shutdown();
        for (Object client : clients) {
            RMIClient.destroy(client, force);

        }
    }


    private Object selectNext() throws TimeoutException {
        Log.debug("try select next");
        Object selected;
        int trial = 1;
        do {
            synchronized (this) {
                selected = routePolicy.selectNext(clients, lastProxy);
            }
            if(selected == null) {
                Log.debug("wait for next proxy available");
                try {
                    Thread.sleep(AVAILABILITY_WAIT_TIMEOUT);
                } catch (InterruptedException e) {
                    Log.error("",e);
                    return null;
                }
            } else {
                return selected;
            }
        } while(trial-- > 0);
        throw new TimeoutException("No Available Service");
    }


    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        lastProxy = selectNext();
        Preconditions.checkNotNull(lastProxy);
        try {
            return method.invoke(lastProxy, args);
        } catch (RMIException e) {
            if(RMIError.isServiceBad(e.code())) {
                purgeBadProxy(lastProxy);
            }
            // rethrow it
            throw e;
        }
    }


    private synchronized void purgeBadProxy(Object badProxy) {
        clients.remove(badProxy);
        final RMIClient client = RMIClient.access(badProxy);
        if(!discoveredProxySet.remove(client.who())) {
            Log.warn("client ({}) is not in the discovered set", client.who());
        }
        RMIClient.destroy(badProxy, true);
        listenerInvoker.submit(new Runnable() {
            @Override
            public void run() {
                synchronized (client) {
                    availabilityChangeListener.onAvailabilityChanged(clients.size());
                    client.notifyAll();
                }
            }
        });
    }

}
