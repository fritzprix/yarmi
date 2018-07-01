package com.doodream.rmovjs.sdp;

import com.doodream.rmovjs.model.RMIServiceInfo;
import com.doodream.rmovjs.net.RMIServiceProxy;
import com.doodream.rmovjs.net.ServiceAdapter;
import com.doodream.rmovjs.net.ServiceProxyFactory;
import com.doodream.rmovjs.serde.Converter;
import com.google.common.base.Preconditions;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
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

    public BaseServiceDiscovery(long interval, TimeUnit unit) {
        tickIntervalInMilliSec = unit.toMillis(interval);
        disposableMap = new HashMap<>();
    }

    @Override
    public void startDiscovery(@NonNull Class service, @NonNull ServiceDiscoveryListener listener) throws InstantiationException, IllegalAccessException {
        startDiscovery(service, listener, TIMEOUT_IN_SEC, TimeUnit.SECONDS);
    }


    private Observable<Long> observeTick() {
        return Observable.interval(0L, tickIntervalInMilliSec, TimeUnit.MILLISECONDS);
    }

    @Override
    public void startDiscovery(@NonNull Class service, @NonNull ServiceDiscoveryListener listener, long timeout, @NonNull TimeUnit unit) throws IllegalAccessException, InstantiationException {
        if(disposableMap.containsKey(service)) {
            return;
        }
        final RMIServiceInfo info = RMIServiceInfo.from(service);
        Converter converter = (Converter) info.getConverter().newInstance();
        Preconditions.checkNotNull(info, "Invalid service type %s", service);
        Preconditions.checkNotNull(converter, "converter is not declared");

        HashSet<RMIServiceInfo> discoveryCache = new HashSet<>();
        listener.onDiscoveryStarted();

        Observable<RMIServiceInfo> serviceInfoObservable = observeTick()
                .map(seq -> receiveServiceInfo(converter))
                .doOnNext(svcInfo -> Log.trace("received info : {}", svcInfo))
                .onErrorReturn(throwable -> RMIServiceInfo.builder().build())
                .filter(discoveryCache::add)
                .filter(info::equals)
                .doOnNext(discovered -> Log.debug("Discovered New Service : {} @ {}", discovered.getName(), discovered.getProxyFactoryHint()))
                .doOnNext(info::copyFrom)
                .timeout(timeout, unit);


        disposableMap.put(service, serviceInfoObservable
                .map(RMIServiceInfo::getAdapter)
                .map(Class::newInstance)
                .cast(ServiceAdapter.class)
                .map(serviceAdapter -> serviceAdapter.getProxyFactory(info))
                .map(ServiceProxyFactory::build)
                .doOnDispose(() -> {
                    close();
                    disposableMap.remove(service);
                    listener.onDiscoveryFinished();

                })
                .doOnError(throwable -> {
                    if(throwable instanceof TimeoutException) {
                        Log.debug("Discovery Timeout");
                    } else {
                        Log.warn("{}", throwable);
                    }
                    close();
                    disposableMap.remove(service);
                    listener.onDiscoveryFinished();
                })
                .doOnComplete(() -> {
                    close();
                    disposableMap.remove(service);
                    listener.onDiscoveryFinished();
                })
                .onErrorReturn(throwable -> RMIServiceProxy.NULL_PROXY)
                .filter(proxy -> !RMIServiceProxy.NULL_PROXY.equals(proxy))
                .subscribe(listener::onDiscovered));

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
