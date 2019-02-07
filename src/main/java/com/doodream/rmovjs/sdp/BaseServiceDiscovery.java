package com.doodream.rmovjs.sdp;

import com.doodream.rmovjs.model.RMIServiceInfo;
import com.doodream.rmovjs.serde.Converter;
import com.google.common.base.Preconditions;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public abstract class BaseServiceDiscovery implements ServiceDiscovery {

    private static final Logger Log = LoggerFactory.getLogger(BaseServiceDiscovery.class);

    protected interface DiscoveryEventListener {
        void onStart();
        void onDiscovered(RMIServiceInfo info);
        void onError(Throwable e);
        void onStop();
    }


    private static class ServiceInfoSource implements ObservableOnSubscribe<RMIServiceInfo>, DiscoveryEventListener {
        private ObservableEmitter<RMIServiceInfo> emitter;

        @Override
        public void onStart() {
        }

        @Override
        public void onDiscovered(RMIServiceInfo info) {
            emitter.onNext(info);
        }

        @Override
        public void onError(Throwable e) {
            emitter.onError(e);
        }

        @Override
        public void onStop() {
            emitter.onComplete();
        }

        @Override
        public void subscribe(ObservableEmitter<RMIServiceInfo> observableEmitter) throws Exception {
            this.emitter = observableEmitter;
        }
    }


    private static final long TIMEOUT_IN_SEC = 5L;
    private final HashMap<Class, Disposable> disposableMap;

    public BaseServiceDiscovery() {
        disposableMap = new HashMap<>();
    }

    @Override
    public void startDiscovery(Class service, boolean once, InetAddress network, ServiceDiscoveryListener listener) throws InstantiationException, IllegalAccessException {
        startDiscovery(service, once, TIMEOUT_IN_SEC, TimeUnit.SECONDS, network, listener);
    }

    @Override
    public void startDiscovery(Class service, boolean once, long timeout, TimeUnit unit, InetAddress network, ServiceDiscoveryListener listener) throws IllegalAccessException, InstantiationException {
        if(disposableMap.containsKey(service)) {
            Log.warn("discovery is already running for service {}", service);
            return;
        }
        final RMIServiceInfo info = RMIServiceInfo.from(service);
        final Converter converter = (Converter) info.getConverter().newInstance();
        Preconditions.checkNotNull(info, "Invalid service type %s", service);
        Preconditions.checkNotNull(converter, "converter is not declared");

        final HashSet<RMIServiceInfo> discoveryCache = new HashSet<>();
        final ServiceInfoSource serviceInfoSource = new ServiceInfoSource();

        onStartDiscovery(serviceInfoSource, network);
        listener.onDiscoveryStarted();

        disposableMap.put(service, Observable.create(serviceInfoSource)
                .onErrorReturn(throwable -> RMIServiceInfo.builder().build())
                .filter(discovered -> {
                    Log.trace("received info : {}", discovered);
                    if(!once) {
                        return true;
                    }
                    return discoveryCache.add(discovered);
                })
                .filter(rmiServiceInfo -> info.equals(rmiServiceInfo))
                .doOnNext(discovered -> {
                    Log.debug("Discovered New Service : {} @ {}", discovered.getName(), discovered.getProxyFactoryHint());
                    info.copyFrom(discovered);
                })
                .timeout(timeout, unit)
                .doOnDispose(() -> {
                    onStopDiscovery();
                    disposableMap.remove(service);
                    listener.onDiscoveryFinished();
                })
                .subscribeOn(Schedulers.io())
                .subscribe(rmiServiceInfo -> listener.onDiscovered(rmiServiceInfo), throwable -> {
                    if (throwable instanceof TimeoutException) {
                        Log.debug("Discovery Timeout");
                    } else {
                        Log.warn("{}", throwable);
                    }
                    onStopDiscovery();
                    disposableMap.remove(service);
                    listener.onDiscoveryFinished();
                }, () -> {
                    onStopDiscovery();
                    disposableMap.remove(service);
                    listener.onDiscoveryFinished();
                }));
    }

    @Override
    public void startDiscovery(@NonNull Class service, boolean once, @NonNull ServiceDiscoveryListener listener) throws InstantiationException, IllegalAccessException, UnknownHostException {
        startDiscovery(service, once, TIMEOUT_IN_SEC, TimeUnit.SECONDS, listener);
    }


    @Override
    public void startDiscovery(@NonNull final Class service, final boolean once, long timeout, @NonNull TimeUnit unit, @NonNull final ServiceDiscoveryListener listener) throws IllegalAccessException, InstantiationException, UnknownHostException {
        InetAddress localHost = InetAddress.getLocalHost();
        startDiscovery(service, once, timeout, unit, localHost, listener);
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

    protected abstract void onStartDiscovery(DiscoveryEventListener listener, InetAddress network);
    protected abstract void onStopDiscovery();
}
