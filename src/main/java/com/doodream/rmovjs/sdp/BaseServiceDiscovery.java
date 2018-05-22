package com.doodream.rmovjs.sdp;

import com.doodream.rmovjs.model.RMIServiceInfo;
import com.doodream.rmovjs.net.RMIServiceProxy;
import com.doodream.rmovjs.net.ServiceAdapter;
import com.doodream.rmovjs.net.ServiceProxyFactory;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import org.apache.logging.log4j.Level;
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
    private HashMap<RMIServiceInfo, Disposable> disposableMap;

    public BaseServiceDiscovery(long interval, TimeUnit unit) {
        tickIntervalinMillisec = unit.toMillis(interval);
        disposableMap = new HashMap<>();
    }

    @Override
    public void startDiscovery(RMIServiceInfo info, ServiceDiscoveryListener listener) {
        startDiscovery(info, listener, TIMEOUT_IN_SEC, TimeUnit.SECONDS);
    }


    private Observable<Long> observeTick() {
        return Observable.interval(0L, tickIntervalinMillisec, TimeUnit.MILLISECONDS);
    }

    @Override
    public void startDiscovery(RMIServiceInfo info, ServiceDiscoveryListener listener, long timeout, TimeUnit unit) {
        HashSet<RMIServiceInfo> discoveryCache = new HashSet<>();

        Observable<RMIServiceInfo> serviceInfoObservable = observeTick()
                .map(seq -> recvServiceInfo())
                .onErrorReturn(throwable -> RMIServiceInfo.builder().build())
                .filter(discoveryCache::add)
                .filter(info::equals)
                .doOnNext(info::copyFrom)
                .timeout(timeout, unit);


        disposableMap.put(info, serviceInfoObservable
                .map(RMIServiceInfo::getAdapter)
                .map(Class::newInstance)
                .cast(ServiceAdapter.class)
                .map(serviceAdapter -> serviceAdapter.getProxyFactory(info))
                .map(ServiceProxyFactory::build)
                .doOnDispose(() -> {
                    listener.onDiscoveryFinished();
                    close();
                })
                .doOnError(throwable -> {
                    listener.onDiscoveryFinished();
                    close();
                })
                .doOnComplete(() -> {
                    listener.onDiscoveryFinished();
                    close();
                })
                .onErrorReturn(throwable -> RMIServiceProxy.NULL_PROXY)
                .filter(proxy -> !RMIServiceProxy.NULL_PROXY.equals(proxy))
                .subscribe(listener::onDiscovered));

    }


    @Override
    public void cancelDiscovery(RMIServiceInfo info) {
        Disposable disposable = disposableMap.get(info);
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
