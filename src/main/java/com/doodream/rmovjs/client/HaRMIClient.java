package com.doodream.rmovjs.client;

import com.doodream.rmovjs.annotation.RMIException;
import com.doodream.rmovjs.annotation.server.Controller;
import com.doodream.rmovjs.annotation.server.Service;
import com.doodream.rmovjs.method.RMIMethod;
import com.doodream.rmovjs.model.Endpoint;
import com.doodream.rmovjs.model.RMIServiceInfo;
import com.doodream.rmovjs.net.RMIServiceProxy;
import com.doodream.rmovjs.sdp.ServiceDiscovery;
import com.doodream.rmovjs.sdp.ServiceDiscoveryListener;
import com.google.common.base.Preconditions;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.TimeUnit;

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
                int lastIdx = proxies.indexOf(lastSelected) + 1;
                if(proxies.size() <= lastIdx) {
                    lastIdx = 0;
                }
                return proxies.get(lastIdx);
            }
        },
        FastestFirst {
            @Override
            public Object selectNext(List<Object> proxies, Object lastSelected) {
                return Observable.fromIterable(proxies)
                        .sorted(Comparator.comparing(RMIClient::access))
                        .blockingFirst();
            }
        };

        public abstract Object selectNext(List<Object> proxies, Object lastSelected);
    }

    private static final long DEFAULT_QOS_UPDATE_PERIOD = 2000L;
    private static final Logger Log = LoggerFactory.getLogger(HaRMIClient.class);

    private Map<Method, Endpoint> methodMap;
    private RequestRoutePolicy routePolicy;
    private CompositeDisposable compositeDisposable;
    private Object lastProxy;
    private long qosFactor;
    private HashSet<String> discoveredProxySet;
    private ArrayList<Object> clients;
    private Class<T> controller;
    private Class svc;
    private long qosUpdateTime;
    private TimeUnit qosUpdateTimeUnit;

    public static <T> T create(ServiceDiscovery discovery, long qos, Class svc, Class<T> ctrl, RequestRoutePolicy policy) {

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


        CompositeDisposable compositeDisposable = new CompositeDisposable();
        compositeDisposable.add(startDiscovery(discovery, svc)
                .subscribeOn(Schedulers.newThread())
                .subscribe(haRMIClient::registerProxy, haRMIClient::onError));

        haRMIClient.setDisposable(compositeDisposable);


        Observable<Endpoint> endpointObservable = Observable.fromIterable(validMethods)
                .map(method -> Endpoint.create(controller, method));

        Single<HashMap<Method, Endpoint>> hashMapSingle = Observable.fromIterable(validMethods)
                .zipWith(endpointObservable, RMIClient::zipIntoMethodMap)
                .collectInto(new HashMap<>(), RMIClient::collectMethodMap);

        haRMIClient.setMethodEndpointMap(hashMapSingle.blockingGet());

        return (T) Proxy.newProxyInstance(ctrl.getClassLoader(),new Class[]{ ctrl }, haRMIClient);
    }


    private HaRMIClient(Class svc, Class<T> ctrl, long qos, long qosUpdatePeriod, TimeUnit timeUnit, RequestRoutePolicy policy) {
        this.svc = svc;
        this.controller = ctrl;
        this.routePolicy = policy;
        this.qosFactor = qos;
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
            clients.add(RMIClient.create(serviceProxy, svc, controller, qosFactor, qosUpdateTime, qosUpdateTimeUnit));
        }
    }

    private void setMethodEndpointMap(HashMap<Method, Endpoint> map) {
        methodMap = map;
    }

    private static Observable<RMIServiceProxy> startDiscovery(ServiceDiscovery discovery, Class svc) {
        return Observable.create(emitter -> discovery.startDiscovery(svc, false, new ServiceDiscoveryListener() {
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
        }));
    }


    private void close(boolean force) {
        compositeDisposable.dispose();
        clients.forEach(proxy -> RMIClient.destroy(proxy, force));
    }


    private Object selectNext() throws InterruptedException {
        Object selected;
        do {
            synchronized (this) {
                selected = routePolicy.selectNext(clients, lastProxy);
            }
            if(selected == null) {
                Thread.sleep(100L);
            }
        } while(selected != null);
        return selected;
    }


    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        lastProxy = selectNext();
        Preconditions.checkNotNull(lastProxy);

        try {
            return method.invoke(lastProxy, method, args);
        } catch (RMIException e) {
            if(e.code() >= 500) {
                // bad service
                synchronized (this) {
                    purgeBadProxy(lastProxy);
                }
            }
            // rethrow it
            throw e;
        }
    }


    private void purgeBadProxy(Object badProxy) {
        clients.remove(badProxy);
        final RMIClient client = RMIClient.access(badProxy);
        if(!discoveredProxySet.remove(client.who())) {
            Log.warn("client ({}) is not in the discovered set", client.who());
        }
        RMIClient.destroy(badProxy, true);
    }

}
