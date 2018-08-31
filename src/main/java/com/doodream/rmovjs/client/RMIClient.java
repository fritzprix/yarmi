package com.doodream.rmovjs.client;

import com.doodream.rmovjs.annotation.RMIException;
import com.doodream.rmovjs.annotation.server.Controller;
import com.doodream.rmovjs.annotation.server.Service;
import com.doodream.rmovjs.method.RMIMethod;
import com.doodream.rmovjs.model.Endpoint;
import com.doodream.rmovjs.model.RMIError;
import com.doodream.rmovjs.model.RMIServiceInfo;
import com.doodream.rmovjs.model.Response;
import com.doodream.rmovjs.net.RMIServiceProxy;
import com.google.common.base.Preconditions;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Single;
import io.reactivex.functions.*;
import io.reactivex.schedulers.Schedulers;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *  {@link RMIClient} build method invocation proxy from {@link RMIServiceProxy} which is discovered from SDP
 *
 */
public class RMIClient implements InvocationHandler, Comparable<RMIClient>  {


    private static final Logger Log = LoggerFactory.getLogger(RMIClient.class);

    private Map<Method, Endpoint> methodMap;
    private RMIServiceProxy serviceProxy;
    private AtomicInteger ongoingRequestCount;
    private long timeout;
    private Long measuredPing;
    private volatile boolean markToClose;

    private RMIClient(RMIServiceProxy serviceProxy, long timeout,long pingUpdatePeriod, TimeUnit timeUnit) {
        this.serviceProxy = serviceProxy;
        markToClose = false;
        measuredPing = Long.MAX_VALUE;
        this.timeout = timeout;
        ongoingRequestCount = new AtomicInteger(0);
        if(pingUpdatePeriod > 0L) {
            serviceProxy.startPeriodicQosUpdate(timeout, pingUpdatePeriod, timeUnit);
        }
    }

    /**
     * return QoS value measured updated last
     * @param proxy proxy object
     * @return QoS value (defined latency in millisecond from request to response)
     */
    public static long getMeasuredQoS(Object proxy) {
        return forEachClient(proxy)
                .blockingFirst().getMeasuredPing();
    }

    /**
     * check whether there are on-going requests for given RMI call proxy
     * @param proxy RMI proxy which is create by {@link #create(RMIServiceProxy, Class, Class)} or {@link #create(RMIServiceProxy, Class, Class, long, long, TimeUnit)}
     * @return true if there is no on-going request, otherwise false
     */
    public static boolean isClosable(Object proxy) {
        return forEachClient(proxy)
                .blockingFirst().isClosable();
    }


    private void setMethodEndpointMap(Map<Method, Endpoint> map) {
        this.methodMap = map;
    }

    private boolean isClosable() {
        return ongoingRequestCount.get() == 0;
    }

    /**
     * get {@link RMIClient} for given RMI call proxy
     * @param proxy
     * @return
     */
    static RMIClient access(Object proxy) {
        return forEachClient(proxy)
                .blockingFirst();
    }

    /**
     * destroy RMI call proxy and release resources
     * @param proxy RMI call proxy returned by {@link #create(RMIServiceProxy, Class, Class, long, long, TimeUnit)} or {@link #create(RMIServiceProxy, Class, Class)}
     * @param force if true, close regardless its on-going request, otherwise, wait until the all the on-going requests is complete
     */
    public static void destroy(Object proxy, final boolean force) {
        forEachClient(proxy)
                .subscribeOn(Schedulers.io())
                .blockingSubscribe(new Consumer<RMIClient>() {
                    @Override
                    public void accept(RMIClient rmiClient) throws Exception {
                        rmiClient.close(force);
                    }
                });
    }

    private static Observable<RMIClient> forEachClient(final Object proxy) {
        return Observable.create(new ObservableOnSubscribe<RMIClient>() {
            @Override
            public void subscribe(final ObservableEmitter<RMIClient> emitter) throws Exception {
                final Class proxyClass = proxy.getClass();
                if(Proxy.isProxyClass(proxyClass)) {
                    RMIClient client = (RMIClient) Proxy.getInvocationHandler(proxy);
                    emitter.onNext(client);
                } else {
                    Service service = proxy.getClass().getAnnotation(Service.class);
                    if(service == null) {
                        throw new IllegalArgumentException("Invalid Proxy");
                    }
                    Observable.fromArray(proxyClass.getDeclaredFields())
                            .filter(new Predicate<Field>() {
                                @Override
                                public boolean test(Field field) throws Exception {
                                    return field.getAnnotation(Controller.class) != null;
                                }
                            })
                            .map(new Function<Field, Object>() {
                                @Override
                                public Object apply(Field field) throws Exception {
                                    return field.get(proxy);
                                }
                            })
                            .map(new Function<Object, InvocationHandler>() {
                                @Override
                                public InvocationHandler apply(Object proxy) throws Exception {
                                    return Proxy.getInvocationHandler(proxy);
                                }
                            })
                            .cast(RMIClient.class)
                            .blockingSubscribe(new Consumer<RMIClient>() {
                                @Override
                                public void accept(RMIClient rmiClient) throws Exception {
                                    emitter.onNext(rmiClient);
                                }
                            });
                }
                emitter.onComplete();
            }
        });
    }


    /**
     * close method invocation proxy created by {@link #create(RMIServiceProxy, Class, Class)} or {@link #createService(RMIServiceProxy, Class)} method
     * @param proxy returned proxy instance from {@link #create(RMIServiceProxy, Class, Class)} or {@link #createService(RMIServiceProxy, Class)}
     */
    public static void destroy(Object proxy) {
        destroy(proxy, false);
    }

    /**
     *
     * @param serviceProxy
     * @param svc
     * @param <T>
     * @return
     * @throws IllegalAccessError
     * @throws InstantiationException
     * @throws IllegalAccessException
     */
    public static <T> T createService(RMIServiceProxy serviceProxy, Class<T> svc) throws IllegalAccessError, InstantiationException, IllegalAccessException {
        return createService(serviceProxy, svc, 0L, 0L, TimeUnit.MILLISECONDS);
    }

    /**
     * create service proxy instance which contains controller interface as its member fields.
     * @param serviceProxy active service proxy which is obtained from the service discovery
     * @param svc Service definition class
     * @param <T> Type of service class
     * @return proxy instance for service, getter or direct field referencing can be used to access controller
     * @throws IllegalAccessException there is no public constructor for service class
     * @throws InstantiationException if this {@code Class} represents an abstract class,
     *      *          an interface, an array class, a primitive type, or void;
     *      *          or if the class has no nullary constructor;
     *      *          or if the instantiation fails for some other reason.
     */
    public static <T> T createService(final RMIServiceProxy serviceProxy, final Class<T> svc, final long timeout, final long pingInterval, final TimeUnit timeUnit) throws IllegalAccessException, InstantiationException {
        final Object svcProxy = svc.newInstance();
        Observable.fromArray(svc.getDeclaredFields())
                .filter(new Predicate<Field>() {
                    @Override
                    public boolean test(Field field) throws Exception {
                        return field.getAnnotation(Controller.class) != null;
                    }
                })
                .blockingSubscribe(new Consumer<Field>() {
                    @Override
                    public void accept(Field field) throws Exception {
                        Object controller = create(serviceProxy, svc, field.getType(), pingInterval, timeout, timeUnit);
                        field.setAccessible(true);
                        field.set(svcProxy, controller);
                    }
                });

        return (T) svcProxy;
    }

    public static <T> T create(RMIServiceProxy serviceProxy, Class<?> svc, final Class<T> ctrl, long pingTimeout, long pingInterval, TimeUnit timeUnit) {
        Service service = svc.getAnnotation(Service.class);
        Preconditions.checkNotNull(service);
        if(!serviceProxy.provide(ctrl)) {
            return null;
        }

        final Controller controller = Observable.fromArray(svc.getDeclaredFields())
                .filter(new Predicate<Field>() {
                    @Override
                    public boolean test(Field field) throws Exception {
                        return field.getType().equals(ctrl);
                    }
                })
                .map(new Function<Field, Controller>() {
                    @Override
                    public Controller apply(Field field) throws Exception {
                        return field.getAnnotation(Controller.class);
                    }
                })
                .blockingFirst(null);

        final RMIServiceInfo serviceInfo = RMIServiceInfo.from(svc);
        List<Method> validMethods = Observable.fromArray(ctrl.getMethods())
                .filter(new Predicate<Method>() {
                    @Override
                    public boolean test(Method method) throws Exception {
                        return RMIMethod.isValidMethod(method);
                    }
                })
                .toList().blockingGet();


        Preconditions.checkNotNull(controller, "no matched controller");
        Preconditions.checkArgument(ctrl.isInterface());
        Preconditions.checkNotNull(serviceInfo, "Invalid Service Class %s", svc);
        Preconditions.checkArgument(validMethods.size() > 0);

        try {
            if (!serviceProxy.isOpen()) {
                // RMIServiceProxy is opened only once
                serviceProxy.open();
            }

            RMIClient rmiClient = new RMIClient(serviceProxy, pingTimeout, pingInterval, timeUnit);

            Observable<Endpoint> endpointObservable = Observable.fromIterable(validMethods)
                    .map(new Function<Method, Endpoint>() {
                        @Override
                        public Endpoint apply(Method method) throws Exception {
                            return Endpoint.create(controller, method);
                        }
                    });

            Single<HashMap<Method, Endpoint>> hashMapSingle = Observable.fromIterable(validMethods)
                    .zipWith(endpointObservable, new BiFunction<Method, Endpoint, Map<Method, Endpoint>>() {
                        @Override
                        public Map<Method, Endpoint> apply(Method method, Endpoint endpoint) throws Exception {
                            return RMIClient.zipIntoMethodMap(method, endpoint);
                        }
                    })
                    .collectInto(new HashMap<Method, Endpoint>(), new BiConsumer<HashMap<Method, Endpoint>, Map<Method, Endpoint>>() {
                        @Override
                        public void accept(HashMap<Method, Endpoint> hashMap, Map<Method, Endpoint> methodEndpointMap) throws Exception {
                            RMIClient.collectMethodMap(hashMap, methodEndpointMap);
                        }
                    });

            rmiClient.setMethodEndpointMap(hashMapSingle.blockingGet());

            // 18. 7. 31 consider give all the available controller interface to the call proxy
            // main concern is...
            // what happen if there are two methods declared in different interfaces which is identical in parameter & return type, etc.
            // method collision is not properly handled at the moment, simple poc is performed to test
            // https://gist.github.com/fritzprix/ca0ecc08fc3125cde529dd11185be0b9

            return (T) Proxy.newProxyInstance(ctrl.getClassLoader(), new Class[]{ ctrl }, rmiClient);
        } catch (Exception e) {
            Log.error(e.getLocalizedMessage());
            return null;
        }
    }

    /**
     * create call proxy instance corresponding to given controller class
     * @param serviceProxy active service proxy which is obtained from the service discovery
     * @param svc Service definition class
     * @param ctrl controller definition as interface
     * @param <T> type of controller class
     * @return call proxy instance for controller
     */
    @Nullable
    public static <T> T create(RMIServiceProxy serviceProxy, Class<?> svc, Class<T> ctrl) {
        return create(serviceProxy, svc, ctrl, 0L, 0L, TimeUnit.MILLISECONDS);
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        close(true);
    }

    /**
     * collect maps between {@link Method} and @{@link Endpoint} into single map
     * @param into map collecting fragmented (or partial) map of {@link Method} and {@link Endpoint}
     * @param methodEndpointMap fragmented (or partial) map of {@link Method} and {@link Endpoint}
     */
    static void collectMethodMap(Map<Method, Endpoint> into, Map<Method, Endpoint> methodEndpointMap) {
        into.putAll(methodEndpointMap);
    }

    /**
     *
     * @param method
     * @param endpoint
     * @return
     */
    static Map<Method, Endpoint> zipIntoMethodMap(Method method, Endpoint endpoint) {
        Map<Method, Endpoint> methodEndpointMap = new HashMap<>();
        methodEndpointMap.put(method, endpoint);
        return methodEndpointMap;
    }

    /**
     * close service proxy used by this call proxy
     * @param force if false, wait until on-going request complete, otherwise, close immediately
     * @throws IOException proxy is already closed,
     */
    private void close(boolean force) throws IOException {
        markToClose = true;
        if(!force) {
            try {
                while (!isClosable()) {
                    // wait until proxy is closable
                    Thread.sleep(10L);
                }
            } catch (InterruptedException ignored) { }
        }

        serviceProxy.close();
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if(markToClose) {
            // prevent new request from being made
            throw new RMIException(RMIError.CLOSED.getResponse());
        }
        Endpoint endpoint = methodMap.get(method);
        if(endpoint == null) {
            return null;
        }
        ongoingRequestCount.getAndIncrement();
        Response response = serviceProxy.request(endpoint, timeout, args);
        ongoingRequestCount.decrementAndGet();
        if(response.isSuccessful()) {
            return response;
        }
        throw new RMIException(response);
    }

    @Override
    public int compareTo(@NotNull RMIClient o) {
        return Math.toIntExact(getMeasuredPing() - o.getMeasuredPing());
    }

    private long getMeasuredPing() {
        return measuredPing;
    }

    String who() {
        return serviceProxy.who();
    }
}
