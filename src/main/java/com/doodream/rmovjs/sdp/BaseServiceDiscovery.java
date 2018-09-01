package com.doodream.rmovjs.sdp;

import com.doodream.rmovjs.model.RMIServiceInfo;
import com.doodream.rmovjs.net.ClientSocketAdapter;
import com.doodream.rmovjs.net.RMIServiceProxy;
import com.doodream.rmovjs.net.ServiceAdapter;
import com.doodream.rmovjs.net.ServiceProxyFactory;
import com.doodream.rmovjs.serde.Converter;
import com.doodream.rmovjs.server.RMIService;
import com.google.common.base.Preconditions;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public abstract class BaseServiceDiscovery implements ServiceDiscovery {

    private static final Logger Log = LoggerFactory.getLogger(BaseServiceDiscovery.class);

    private static final long TIMEOUT_IN_SEC = 5L;
    private long tickIntervalInMilliSec;
    private HashMap<Class, Disposable> disposableMap;

    BaseServiceDiscovery(long interval, TimeUnit unit) {
        tickIntervalInMilliSec = unit.toMillis(interval);
        disposableMap = new HashMap<>();
    }

    @Override
    public void startDiscovery(@NonNull Class service, boolean once, @NonNull ServiceDiscoveryListener listener) throws InstantiationException, IllegalAccessException {
        startDiscovery(service, once, TIMEOUT_IN_SEC, TimeUnit.SECONDS, listener);
    }


    private Observable<Long> observeTick() {
        return Observable.interval(0L, tickIntervalInMilliSec, TimeUnit.MILLISECONDS);
    }

    @Override
    public void startDiscovery(@NonNull final Class service, final boolean once, long timeout, @NonNull TimeUnit unit, @NonNull final ServiceDiscoveryListener listener) throws IllegalAccessException, InstantiationException {
        if(disposableMap.containsKey(service)) {
            return;
        }
        final RMIServiceInfo info = RMIServiceInfo.from(service);
        final Converter converter = (Converter) info.getConverter().newInstance();
        Preconditions.checkNotNull(info, "Invalid service type %s", service);
        Preconditions.checkNotNull(converter, "converter is not declared");

        final HashSet<RMIServiceInfo> discoveryCache = new HashSet<>();
        listener.onDiscoveryStarted();

        Observable<RMIServiceInfo> serviceInfoObservable = observeTick()
                .map(new Function<Long, RMIServiceInfo>() {
                    @Override
                    public RMIServiceInfo apply(Long aLong) throws Exception {
                        return receiveServiceInfo(converter);
                    }
                })
                .doOnNext(new Consumer<RMIServiceInfo>() {
                    @Override
                    public void accept(RMIServiceInfo svcInfo) throws Exception {
                        Log.trace("received info : {}", svcInfo);
                    }
                })
                .onErrorReturn(new Function<Throwable, RMIServiceInfo>() {
                    @Override
                    public RMIServiceInfo apply(Throwable throwable) throws Exception {
                        return RMIServiceInfo.builder().build();
                    }
                })
                .filter(new Predicate<RMIServiceInfo>() {
                    @Override
                    public boolean test(RMIServiceInfo discovered) throws Exception {
                        if(!once) {
                            return true;
                        }
                        return discoveryCache.add(discovered);
                    }
                })
                .filter(new Predicate<RMIServiceInfo>() {
                    @Override
                    public boolean test(RMIServiceInfo discovered) throws Exception {
                        if(!once) {
                            return true;
                        }
                        return discoveryCache.add(discovered);                    }
                })
                .filter(new Predicate<RMIServiceInfo>() {
                    @Override
                    public boolean test(RMIServiceInfo rmiServiceInfo) throws Exception {
                        return info.equals(rmiServiceInfo);
                    }
                })
                .doOnNext(new Consumer<RMIServiceInfo>() {
                    @Override
                    public void accept(RMIServiceInfo discovered) throws Exception {
                        Log.debug("Discovered New Service : {} @ {}", discovered.getName(), discovered.getProxyFactoryHint());
                        info.copyFrom(discovered);
                    }
                })
                .timeout(timeout, unit);


        disposableMap.put(service, serviceInfoObservable
                .map(new Function<RMIServiceInfo, Class<?>>() {
                    @Override
                    public Class<?> apply(RMIServiceInfo rmiServiceInfo) throws Exception {
                        return rmiServiceInfo.getAdapter();
                    }
                })
                .map(new Function<Class<?>, Object>() {
                    @Override
                    public Object apply(Class<?> cls) throws Exception {
                        return cls.newInstance();
                    }
                })
                .cast(ServiceAdapter.class)
                .map(new Function<ServiceAdapter, ServiceProxyFactory>() {
                    @Override
                    public ServiceProxyFactory apply(ServiceAdapter serviceAdapter) throws Exception {
                        return serviceAdapter.getProxyFactory(info);
                    }
                })
                .map(new Function<ServiceProxyFactory, RMIServiceProxy>() {
                    @Override
                    public RMIServiceProxy apply(ServiceProxyFactory serviceProxyFactory) throws Exception {
                        return serviceProxyFactory.build();
                    }
                })
                .doOnDispose(new Action() {
                    @Override
                    public void run() throws Exception {
                        close();
                        disposableMap.remove(service);
                        listener.onDiscoveryFinished();
                    }
                })
                .doOnError(new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        if(throwable instanceof TimeoutException) {
                            Log.debug("Discovery Timeout");
                        } else {
                            Log.warn("{}", throwable);
                        }
                        close();
                        disposableMap.remove(service);
                        listener.onDiscoveryFinished();
                    }
                })
                .doOnComplete(new Action() {
                    @Override
                    public void run() throws Exception {
                        close();
                        disposableMap.remove(service);
                        listener.onDiscoveryFinished();
                    }
                })
                .onErrorReturn(new Function<Throwable, RMIServiceProxy>() {
                    @Override
                    public RMIServiceProxy apply(Throwable throwable) throws Exception {
                        return RMIServiceProxy.NULL_PROXY;
                    }
                })
                .filter(new Predicate<RMIServiceProxy>() {
                    @Override
                    public boolean test(RMIServiceProxy proxy) throws Exception {
                        return !RMIServiceProxy.NULL_PROXY.equals(proxy);
                    }
                })
                .subscribe(new Consumer<RMIServiceProxy>() {
                    @Override
                    public void accept(RMIServiceProxy rmiServiceProxy) throws Exception {
                        listener.onDiscovered(rmiServiceProxy);
                    }
                }));

    }


    @Override
    public void cancelDiscovery(Class service) {
        Disposable disposable = disposableMap.get(service);
        if(disposable == null) {
            return;
        }
        if(disposable.isDisposed()) {
            return;
        }
        disposable.dispose();
    }


    protected abstract RMIServiceInfo receiveServiceInfo(Converter converter) throws IOException;
    protected abstract void close();
}
