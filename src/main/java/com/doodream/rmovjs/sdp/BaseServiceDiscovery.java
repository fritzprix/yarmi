package com.doodream.rmovjs.sdp;

import com.doodream.rmovjs.model.RMIServiceInfo;
import com.doodream.rmovjs.net.RMIServiceProxy;
import com.doodream.rmovjs.net.ServiceAdapter;
import com.doodream.rmovjs.net.ServiceProxyFactory;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;

public abstract class BaseServiceDiscovery implements ServiceDiscovery {

    private static final Logger LOGGER = LogManager.getLogger(BaseServiceDiscovery.class);

    private static final long TIMEOUT_IN_SEC = 5L;
    private long tickIntervalinMillisec;
    private HashMap<Class, Disposable> disposableMap;

    public BaseServiceDiscovery(long interval, TimeUnit unit) {
        tickIntervalinMillisec = unit.toMillis(interval);
        disposableMap = new HashMap<>();
    }

    @Override
    public void startDiscovery(Class service, ServiceDiscoveryListener listener) {
        startDiscovery(service, listener, TIMEOUT_IN_SEC, TimeUnit.SECONDS);
    }


    private Observable<Long> observeTick() {
        return Observable.interval(0L, tickIntervalinMillisec, TimeUnit.MILLISECONDS);
    }

    @Override
    public void startDiscovery(Class service, ServiceDiscoveryListener listener, long timeout, TimeUnit unit) {
        if(disposableMap.containsKey(service)) {
            return;
        }
        final RMIServiceInfo info = RMIServiceInfo.from(service);
        HashSet<RMIServiceInfo> discoveryCache = new HashSet<>();

        Observable<RMIServiceInfo> serviceInfoObservable = observeTick()
                .map(seq -> recvServiceInfo())
                .onErrorReturn(throwable -> RMIServiceInfo.builder().build())
                .filter(discoveryCache::add)
                .filter(info::equals)
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


    protected abstract RMIServiceInfo recvServiceInfo() throws IOException;
    protected abstract void close();
}
