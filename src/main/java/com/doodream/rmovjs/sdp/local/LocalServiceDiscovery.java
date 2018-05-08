package com.doodream.rmovjs.sdp.local;

import com.doodream.rmovjs.model.ServiceInfo;
import com.doodream.rmovjs.net.RMIServiceProxy;
import com.doodream.rmovjs.net.inet.InetRMISocket;
import com.doodream.rmovjs.net.inet.InetServiceProxy;
import com.doodream.rmovjs.sdp.ServiceDiscovery;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import org.reactivestreams.Subscription;

import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class LocalServiceDiscovery implements ServiceDiscovery {
    private ExecutorService singleThreadedExecutor;
    private HashMap<Thread, Disposable> disposableHashMap = new HashMap<>();

    @Override
    public Observable<RMIServiceProxy> discover(ServiceInfo info, long timeout, TimeUnit unit) throws IOException {
        return Observable.intervalRange(0L, 100L, 0L, 10L, TimeUnit.SECONDS)
                .map(aLong -> this.fromHeartBeat(aLong, info));

    }

    @Override
    public Observable<RMIServiceProxy> startDiscovery(ServiceInfo info) throws IOException {

        return Observable.interval(0L, 10L, TimeUnit.SECONDS)
                .map(aLong -> this.fromHeartBeat(aLong, info)).doOnSubscribe(disposable -> {

                    final Thread current = Thread.currentThread();
                    if(disposableHashMap.containsKey(current)) {
                        Disposable subscription = disposableHashMap.get(current);
                        if(!subscription.isDisposed()) {
                            subscription.dispose();
                        }
                    }
                    disposableHashMap.put(Thread.currentThread(), disposable);
                });
    }

    private <R> RMIServiceProxy fromHeartBeat(Long aLong, ServiceInfo info) {
        return Observable.fromIterable(LocalServiceRegistry.listService())
                .filter(info::equals)
                .doOnNext(found -> System.out.printf("Service Found : %s\n", found))
                .map(found -> new Socket("localhost", 3000))
                .map(InetRMISocket::new)
                .map(socket -> InetServiceProxy.create(info, socket))
                .blockingSingle();
    }


    @Override
    public void stopDiscovery() throws IOException {
        Disposable disposable = disposableHashMap.get(Thread.currentThread());
        if(!disposable.isDisposed()) {
            disposable.dispose();
        }
    }
}
