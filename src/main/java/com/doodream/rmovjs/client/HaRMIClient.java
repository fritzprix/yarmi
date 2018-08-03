package com.doodream.rmovjs.client;

import com.doodream.rmovjs.annotation.server.Controller;
import com.doodream.rmovjs.annotation.server.Service;
import com.doodream.rmovjs.method.RMIMethod;
import com.doodream.rmovjs.model.Endpoint;
import com.doodream.rmovjs.model.RMIServiceInfo;
import com.doodream.rmovjs.net.RMIServiceProxy;
import com.doodream.rmovjs.sdp.ServiceDiscovery;
import com.doodream.rmovjs.sdp.ServiceDiscoveryListener;
import com.doodream.rmovjs.util.LruCache;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 *  High-Available RMI Client
 */
public class HaRMIClient implements InvocationHandler {


    /**
     * policy
     */
    public enum RequestRoutePolicy {
        RoundRobin,
s    }

    private static final long PING_INTERVAL = 2000L;
    private static final Logger Log = LoggerFactory.getLogger(HaRMIClient.class);

    private String controllerPath;
    private Map<Method, Endpoint> methodMap;
    private RequestRoutePolicy routePolicy;
    private CompositeDisposable compositeDisposable;
    private Class controller;
    private HashSet<String> discoveredProxySet;
    private ServiceDiscovery discovery;

    private LruCache<String, RMIServiceProxy> proxies;

    public static <T> T create(ServiceDiscovery discovery, Class svc, Class<T> ctrl, RequestRoutePolicy policy) throws InstantiationException {

        Service service = (Service) svc.getAnnotation(Service.class);
        Preconditions.checkNotNull(service);


        HaRMIClient haRMIClient = new HaRMIClient(policy);
        CompositeDisposable compositeDisposable = new CompositeDisposable();
        compositeDisposable.add(startDiscovery(discovery, svc)
                .subscribeOn(Schedulers.newThread())
                .subscribe(haRMIClient::onServiceDiscovered,haRMIClient::onError));

        compositeDisposable.add(Observable.interval(PING_INTERVAL, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.newThread())
                .subscribe(haRMIClient::pingActiveService));

        haRMIClient.setDisposable(compositeDisposable);


        Controller controller = Observable.fromArray(svc.getDeclaredFields())
                .filter(field -> field.getType().equals(ctrl))
                .map(field -> field.getAnnotation(Controller.class))
                .blockingFirst(null);


        Preconditions.checkNotNull(controller, "no matched controller");
        Preconditions.checkArgument(ctrl.isInterface());

        haRMIClient.setController(ctrl);

        final RMIServiceInfo serviceInfo = RMIServiceInfo.from(svc);
        Preconditions.checkNotNull(serviceInfo, "Invalid Service Class %s", svc);


        Observable<Method> methodObservable = Observable.fromArray(ctrl.getMethods())
                .filter(RMIMethod::isValidMethod);

        Observable<Endpoint> endpointObservable = methodObservable
                .map(method -> Endpoint.create(controller, method));

        Single<HashMap<Method, Endpoint>> hashMapSingle = methodObservable
                .zipWith(endpointObservable, RMIClient::zipIntoMethodMap)
                .collectInto(new HashMap<>(), RMIClient::collectMethodMap);

        haRMIClient.setMethodEndpointMap(hashMapSingle.blockingGet());

        return (T) Proxy.newProxyInstance(ctrl.getClassLoader(),new Class[]{ctrl}, haRMIClient);
    }

    private void onError(Throwable throwable) {

    }

    private <T> void setController(Class<T> ctrl) {
        controller = ctrl;
    }

    private void setDisposable(CompositeDisposable compositeDisposable) {
        this.compositeDisposable = compositeDisposable;
    }

    private void onServiceDiscovered(RMIServiceProxy serviceProxy) {
        Observable.just(serviceProxy)
                .filter(proxy -> proxy.provide(controller))
                .filter(proxy -> discoveredProxySet.add(proxy.who()))
                .blockingSubscribe(this::addActiveServiceProxy);
    }

    private void addActiveServiceProxy(RMIServiceProxy serviceProxy) {
    }

    private void pingActiveService(Long aLong) {

    }

    private void setMethodEndpointMap(HashMap<Method, Endpoint> map) {
        methodMap = map;
    }

    private static Observable<RMIServiceProxy> startDiscovery(ServiceDiscovery discovery, Class svc) {
        return Observable.create(emitter -> discovery.startDiscovery(svc, new ServiceDiscoveryListener() {
            @Override
            public void onDiscovered(RMIServiceProxy proxy) {
                emitter.onNext(proxy);
            }

            @Override
            public void onDiscoveryStarted() {

            }

            @Override
            public void onDiscoveryFinished() throws IllegalAccessException {
                emitter.onComplete();
            }
        }));
    }

    private HaRMIClient(RequestRoutePolicy policy) {
        this.routePolicy = policy;
        discoveredProxySet =  new HashSet<>();
    }

    private void close() {
        compositeDisposable.dispose();
    }


    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        return null;
    }

}
