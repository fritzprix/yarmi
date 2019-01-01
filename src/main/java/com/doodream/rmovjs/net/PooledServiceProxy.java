package com.doodream.rmovjs.net;

import com.doodream.rmovjs.Properties;
import com.doodream.rmovjs.model.ControllerInfo;
import com.doodream.rmovjs.model.Endpoint;
import com.doodream.rmovjs.model.RMIServiceInfo;
import com.doodream.rmovjs.model.Response;
import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import lombok.Data;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * pooled service proxy that keeps track of multiple service proxies and prioritize proxies along the given policy
 */
public class PooledServiceProxy implements ServiceProxy {
    private static final Logger Log = LoggerFactory.getLogger(PooledServiceProxy.class);
    // endpoint for health check

    private final CompositeDisposable disposables = new CompositeDisposable();
    private final ArrayBlockingQueue<ServiceProxy> proxyRegistryWaitQueue;
    private final ServicePoolingPolicy policy;
    private final List<ManagedServiceProxy> pooledProxies;
    private ExecutorService executorService;
    private volatile boolean isOpened;
    private RMIServiceInfo serviceInfo;

    public enum PrioritizePolicy {
        LOWEST_LATECNY_FIRST {
            @Override
            public Comparator<? super ManagedServiceProxy> rule() {
                return (Comparator<ManagedServiceProxy>) (o1, o2) -> (int) (o1.measuredQoS - o2.measuredQoS);
            }
        },
        LEAST_LOAD_FIRST {
            @Override
            public Comparator<? super ManagedServiceProxy> rule() {
                return new Comparator<ManagedServiceProxy>() {
                    @Override
                    public int compare(ManagedServiceProxy o1, ManagedServiceProxy o2) {
                        return o1.getOngoingRequestCount().get() - o2.getOngoingRequestCount().get();
                    }
                };
            }
        },
        ROUND_ROBIN {
            @Override
            public Comparator<? super ManagedServiceProxy> rule() {
                return super.rule();
            }
        };

        public Comparator<? super ManagedServiceProxy> rule() {
            return null;
        }
    }

    public enum DropPolicy {
        NEVER,
        LAG,
        REQUEST_FAIL
    }

    @Data
    private static class ManagedServiceProxy implements ServiceProxy {
        @NonNull private ServiceProxy serviceProxy;
        private long measuredQoS;
        private AtomicInteger ongoingRequestCount;
        private long touchedTimestamp;

        public static ManagedServiceProxy create(ServiceProxy proxy) {
            ManagedServiceProxy managedServiceProxy = new ManagedServiceProxy(proxy);
            managedServiceProxy.setMeasuredQoS(Long.MAX_VALUE);
            managedServiceProxy.setOngoingRequestCount(new AtomicInteger(0));
            return managedServiceProxy;
        }

        @Override
        public void open() throws IOException, IllegalAccessException, InstantiationException {
            serviceProxy.open();
        }

        @Override
        public boolean isOpen() {
            return serviceProxy.isOpen();
        }

        @Override
        public Response request(Endpoint endpoint, long timeoutMilliSec, Object... args) throws IOException {
            Log.debug("request count {}", ongoingRequestCount.incrementAndGet());
            Response response = serviceProxy.request(endpoint, timeoutMilliSec, args);
            ongoingRequestCount.decrementAndGet();
            touchedTimestamp = System.currentTimeMillis();
            return response;
        }

        @Override
        public void close() throws IOException {
            serviceProxy.close();
        }

        @Override
        public String who() {
            return serviceProxy.who();
        }

        @Override
        public void startQosMeasurement(long interval, long timeout, TimeUnit timeUnit, QosListener listener) {
            serviceProxy.startQosMeasurement(interval, timeout, timeUnit, listener);
        }

        @Override
        public void stopQosMeasurement(QosListener listener) {
            serviceProxy.stopQosMeasurement(listener);
        }

        @Override
        public boolean provide(Class controller) {
            return serviceProxy.provide(controller);
        }
    }

    public static class ServicePoolingPolicy {

        public static class Builder {
            ServicePoolingPolicy policy;
            public Builder() {
                policy = new ServicePoolingPolicy();
            }

            public Builder setPrioritizePolicy(PrioritizePolicy prioritizePolicy) {
                policy.prioritizePolicy = prioritizePolicy;
                return this;
            }

            public Builder setDropPolicy(DropPolicy dropPolicy) {
                policy.dropPolicy = dropPolicy;
                return this;
            }

            public Builder setTimeout(long timeout) {
                policy.rttThreshold = timeout;
                return this;
            }

            public Builder setFailCount(int cnt) {
                policy.maxFailCount = cnt;
                return this;
            }

            public ServicePoolingPolicy build() {
                if(!policy.isValid()) {
                    throw new IllegalStateException("invalid policy");
                }
                return policy;
            }
        }

        private boolean isValid() {
            if((dropPolicy == DropPolicy.LAG) && (rttThreshold <= 0L)) {
                return false;
            }
            if((dropPolicy == DropPolicy.REQUEST_FAIL) && (maxFailCount <= 0)) {
                return false;
            }
            return true;
        }

        private PrioritizePolicy prioritizePolicy;
        private DropPolicy dropPolicy;
        private int maxFailCount = 0;
        private long rttThreshold = 0L;

    }

    public static PooledServiceProxy create(Class service, ServicePoolingPolicy policy) {
        if(!policy.isValid()) {
            throw new IllegalArgumentException("invalid policy");
        }
        final RMIServiceInfo serviceInfo = RMIServiceInfo.from(service);
        return new PooledServiceProxy(serviceInfo, policy);
    }

    private PooledServiceProxy(RMIServiceInfo serviceInfo, ServicePoolingPolicy policy) {
        this.serviceInfo = serviceInfo;
        proxyRegistryWaitQueue = new ArrayBlockingQueue<>(50);
        executorService = Executors.newFixedThreadPool(Properties.getPoolProxyParallelism());
        pooledProxies = new ArrayList<>();
        isOpened = false;
        this.policy = policy;
    }

    public void pool(final ServiceProxy proxy) {

        Observable.fromIterable(serviceInfo.getControllerInfos())
                .map(controllerInfo -> controllerInfo.getStubCls())
                .map(controller -> proxy.provide(controller))
                .reduce((aBoolean, aBoolean2) -> aBoolean && aBoolean2)
                .subscribeOn(Schedulers.io())
                .toObservable()
                .blockingSubscribe(valid -> {
                    try {
                        proxy.open();
                        if(proxyRegistryWaitQueue.offer(proxy, 10L,TimeUnit.SECONDS)) {
                            Log.debug("proxy is pooled successfully");
                            return;
                        }
                    } catch (InterruptedException ignore) {
                        // ignore exception here
                    } catch (IllegalAccessException | IOException | InstantiationException e) {
                        Log.warn("fail to open proxy");
                    }
                    Log.warn("fail to pooling proxy");
                });
    }

    @Override
    public void open() throws IOException, IllegalAccessException, InstantiationException {
        isOpened = true;
        disposables.add(handlePoolingRequest()
                .repeatUntil(() -> isOpened)
                .subscribeOn(Schedulers.single())
                .subscribe(proxy -> {
                    ManagedServiceProxy proxyWrapper = ManagedServiceProxy.create(proxy);
                    synchronized (pooledProxies) {
                        pooledProxies.add(proxyWrapper);
                    }

                    proxy.startQosMeasurement(3L, 10L, TimeUnit.SECONDS, new QosListener() {
                        @Override
                        public void onQosUpdated(ServiceProxy proxy, long measuredRttInMill) {
                            proxyWrapper.setMeasuredQoS(measuredRttInMill);
                            switch (policy.dropPolicy) {
                                case LAG:
                                    if(policy.rttThreshold < measuredRttInMill) {
                                        synchronized (pooledProxies) {
                                            Log.debug("proxy {} will be dropped from ready queue", proxyWrapper.who());
                                            pooledProxies.remove(proxyWrapper);
                                        }
                                    }
                                    break;
                                case NEVER:
                                default:
                            }
                        }

                        @Override
                        public void onError(ServiceProxy proxy, Throwable throwable) {
                            Log.warn("error on proxy {}", proxy.who(), throwable);
                            synchronized (pooledProxies) {
                                Log.debug("proxy {} will be dropped", proxy.who());
                                pooledProxies.remove(proxyWrapper);
                            }
                        }
                    });
                }));
    }

    private Observable<ServiceProxy> handlePoolingRequest() {
        return Observable.create(emitter -> {
            try {
                final ServiceProxy proxy = proxyRegistryWaitQueue.take();
                emitter.onNext(proxy);
            } catch (InterruptedException e) {
                emitter.onError(e);
            }
            emitter.onComplete();
        });
    }

    @Override
    public boolean isOpen() {
        return isOpened;
    }

    @Override
    public Response request(Endpoint endpoint, long timeoutMilliSec, Object... args) throws IOException {
        ManagedServiceProxy proxy = Observable.fromIterable(pooledProxies)
                .sorted(policy.prioritizePolicy.rule())
                .blockingFirst();

        return proxy.request(endpoint, timeoutMilliSec, args);
    }

    @Override
    public void close() throws IOException {
        if(disposables.isDisposed()) {
            return;
        }
        disposables.dispose();
        disposables.clear();
    }

    @Override
    public String who() {
        return Observable.fromIterable(pooledProxies)
                .map(proxy -> proxy.hashCode())
                .reduce((integer, integer2) -> integer + integer2)
                .map(v -> Integer.toHexString(v))
                .blockingGet();
    }

    @Override
    public void startQosMeasurement(long interval, long timeout, TimeUnit timeUnit, QosListener listener) {

    }

    @Override
    public void stopQosMeasurement(QosListener listener) {

    }


    @Override
    public boolean provide(Class controller) {
        for (ControllerInfo controllerInfo : serviceInfo.getControllerInfos()) {
            if(controllerInfo.getStubCls().equals(controller)) {
                return true;
            }
        }
        return false;
    }
}
