package com.doodream.rmovjs.client;

import com.doodream.rmovjs.annotation.server.Service;
import com.doodream.rmovjs.model.Endpoint;
import com.doodream.rmovjs.model.RMIServiceInfo;
import com.doodream.rmovjs.net.RMIServiceProxy;
import com.doodream.rmovjs.sdp.ServiceDiscovery;
import com.doodream.rmovjs.sdp.ServiceDiscoveryListener;
import com.doodream.rmovjs.serde.Converter;
import com.doodream.rmovjs.util.LruCache;
import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 *  High-Available RMI Client
 */
public class HaRMIClient implements InvocationHandler, Consumer<RMIServiceProxy> {



    /**
     * policy
     */
    public enum RequestRoutePolicy {
        RoundRobin,
s    }

    private static final Logger Log = LoggerFactory.getLogger(HaRMIClient.class);

    private String controllerPath;
    private Map<Method, Endpoint> methodMap;
    private RMIServiceProxy serviceProxy;
    private RMIServiceInfo serviceInfo;
    private Converter converter;

    private ServiceDiscovery discovery;
    private CompositeDisposable compositeDisposable;
    private LruCache<String, RMIServiceProxy> proxies;

    public static <T> T create(ServiceDiscovery discovery, Class svc, Class<T> ctrl) throws IllegalAccessException, IOException, InstantiationException {

        Service service = (Service) svc.getAnnotation(Service.class);
        if(service == null) {
            return null;
        }

        HaRMIClient haRMIClient = new HaRMIClient();
        CompositeDisposable compositeDisposable = new CompositeDisposable();
        compositeDisposable.add(startDiscovery(discovery, svc)
                .subscribeOn(Schedulers.newThread())
                .subscribe(haRMIClient::accept));


        return null;
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

    private HaRMIClient() {
    }


    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        return null;
    }

    @Override
    public void accept(RMIServiceProxy serviceProxy) {

    }
}
