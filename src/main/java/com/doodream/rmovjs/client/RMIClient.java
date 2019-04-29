package com.doodream.rmovjs.client;

import com.doodream.rmovjs.annotation.RMIException;
import com.doodream.rmovjs.annotation.server.Controller;
import com.doodream.rmovjs.annotation.server.Service;
import com.doodream.rmovjs.method.RMIMethod;
import com.doodream.rmovjs.model.Endpoint;
import com.doodream.rmovjs.model.RMIError;
import com.doodream.rmovjs.model.RMIServiceInfo;
import com.doodream.rmovjs.model.Response;
import com.doodream.rmovjs.net.ServiceProxy;
import com.google.common.base.Preconditions;
import io.reactivex.Observable;
import io.reactivex.Single;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *  {@link RMIClient} build method invocation proxy from {@link ServiceProxy} which is discovered from SDP
 *
 */
public class RMIClient implements InvocationHandler, Comparable<RMIClient>  {


    private static final Logger Log = LoggerFactory.getLogger(RMIClient.class);

    private Map<Method, Endpoint> methodMap;
    private ServiceProxy serviceProxy;
    private final AtomicInteger ongoingRequestCount;
    private long timeout;
    private Long responseTime;
    private volatile boolean markToClose;

    private RMIClient(ServiceProxy serviceProxy, long timeout) {
        this.serviceProxy = serviceProxy;
        markToClose = false;
        responseTime = Long.MAX_VALUE;
        this.timeout = timeout;
        ongoingRequestCount = new AtomicInteger(0);
    }

    /**
     * check whether there are on-going requests for given RMI call proxy
     * @return true if there is no on-going request, otherwise false
     */
    public static boolean isClosable(Object proxy) {
        return access(proxy).isClosable();
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
        if(Proxy.isProxyClass(proxy.getClass())) {
            throw new IllegalArgumentException(String.format(Locale.ENGLISH,"invalid proxy : %s", proxy.getClass().getName()));
        }
        return  (RMIClient) Proxy.getInvocationHandler(proxy);
    }

    /**
     * destroy RMI call proxy and release resources
     * @param force if true, close regardless its on-going request, otherwise, wait until the all the on-going requests is complete
     */
    public static void destroy(Object proxy, boolean force) throws IOException {
        if(!Proxy.isProxyClass(proxy.getClass())) {
            throw new IllegalArgumentException(String.format(Locale.ENGLISH,"invalid proxy : %s", proxy.getClass().getName()));
        }
        RMIClient client = (RMIClient) Proxy.getInvocationHandler(proxy);
        client.close(force);
    }

    /**
     *
     * @param proxy
     * @throws IOException
     */
    public static void destroy(Object proxy) throws IOException {
        destroy(proxy, false);
    }

    static RMIClient createClient(ServiceProxy serviceProxy, Class<?> svc, Class<?>[] ctrl, long timeoutInMills) {
        Service service = svc.getAnnotation(Service.class);
        Preconditions.checkNotNull(service);
        final ConcurrentHashMap<Class<?>, Controller> controllerMap  = new ConcurrentHashMap<>();

        final Set<Field> controllers = Observable.fromArray(svc.getDeclaredFields())
                .filter(field -> isAnnotatedWithController(field))
                .filter(field -> serviceProxy.provide(field.getType()))
                .collectInto(new HashSet<Field>(), (fields, field) -> fields.add(field))
                .blockingGet();
        if(ctrl == null) {
            // all the controllers declared are added to call proxy, if ctrl is given as null
            Observable.fromIterable(controllers)
                    .blockingSubscribe(field -> controllerMap.put(field.getType(), field.getAnnotation(Controller.class)));
        } else {
            Set<Class<?>> controllerSet = new HashSet<>(Arrays.asList(ctrl));
            Observable.fromIterable(controllers)
                    .blockingSubscribe(field -> {
                        if(controllerSet.contains(field.getType())) {
                            Log.debug("matched controller {}", field.getType().getSimpleName());
                            controllerMap.put(field.getType(), field.getAnnotation(Controller.class));
                        }
                    });
        }
        if(controllerMap.isEmpty()) {
            // throw IllegalArgumentException if there is no matched controller
            throw new IllegalArgumentException(String.format(Locale.ENGLISH, "no valid controllers for %s", service.name()));
        }

        final RMIServiceInfo serviceInfo = RMIServiceInfo.from(svc);
        Set<Method> validMethods = Observable.fromIterable(controllerMap.entrySet())
                .flatMap(entry -> extractRMIMethods(entry.getKey()))
                .collectInto(new HashSet<Method>(), (methods, method) -> methods.add(method))
                .blockingGet();

        Preconditions.checkNotNull(serviceInfo, "Invalid Service Class %s", svc);
        Preconditions.checkArgument(validMethods.size() > 0);

        try {
            if(serviceProxy.open()) {
                Log.debug("{} is newly opened", serviceProxy.who());
            }

            RMIClient rmiClient = new RMIClient(serviceProxy, timeoutInMills);

            Observable<Endpoint> endpointObservable = Observable.fromIterable(validMethods)
                    .map(method -> Endpoint.create(controllerMap.get(method.getDeclaringClass()), method));

            Single<HashMap<Method, Endpoint>> hashMapSingle = Observable.fromIterable(validMethods)
                    .zipWith(endpointObservable, RMIClient::zipIntoMethodMap)
                    .collectInto(new HashMap<>(), RMIClient::collectMethodMap);

            rmiClient.setMethodEndpointMap(hashMapSingle.blockingGet());

            // 18. 7. 31 consider give all the available controller interface to the call proxy
            // main concern is...
            // what happen if there are two methods declared in different interfaces which is identical in parameter & return type, etc.
            // method collision is not properly handled at the moment, simple poc is performed to test
            // https://gist.github.com/fritzprix/ca0ecc08fc3125cde529dd11185be0b9

            return rmiClient;
        } catch (Exception e) {
            Log.error("", e);
            return null;
        }
    }

    private static Observable<Method> extractRMIMethods(Class<?> cls) {
        return Observable.fromArray(cls.getMethods())
                .filter(method -> RMIMethod.isValidMethod(method));
    }

    private static boolean isAnnotatedWithController(Field field) {
        return field.getAnnotation(Controller.class) != null;
    }

    /**
     *
     * @param serviceProxy
     * @param svc
     * @param ctrl
     * @param timeout
     * @param timeUnit
     * @return
     */
    public static Object create(ServiceProxy serviceProxy, Class<?> svc, Class<?>[] ctrl, long timeout, TimeUnit timeUnit) {
        return create(serviceProxy, svc, ctrl, timeout);
    }

    /**
     *
     * @param serviceProxy
     * @param svc
     * @param ctrl
     * @param timeoutInMills
     * @return
     */
    public static Object create(ServiceProxy serviceProxy, Class<?> svc, Class<?>[] ctrl, long timeoutInMills) {
        RMIClient rmiClient = createClient(serviceProxy, svc, ctrl, timeoutInMills);
        if(rmiClient == null) {
            return null;
        }
        return Proxy.newProxyInstance(svc.getClassLoader(), ctrl, rmiClient);
    }

    /**
     * create call proxy instance corresponding to given controller class
     * @param serviceProxy active service proxy which is obtained from the service discovery
     * @param svc Service definition class
     * @param ctrl controller definition as interface
     * @return call proxy instance for controller
     */
    public static Object create(ServiceProxy serviceProxy, Class<?> svc, Class<?>[] ctrl) {
        return create(serviceProxy, svc, ctrl, 0L);
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
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
    void close(boolean force) throws IOException {
        markToClose = true;
        if(!force) {
            try {
                synchronized (ongoingRequestCount) {
                    while (!isClosable()) {
                        ongoingRequestCount.wait();
                        // wait until proxy is closable
                    }
                }
            } catch (InterruptedException ignored) { }

        }
        serviceProxy.close(force);
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
        synchronized (ongoingRequestCount) {
            ongoingRequestCount.decrementAndGet();
            ongoingRequestCount.notifyAll();
        }
        if(response.isSuccessful()) {
            return response;
        }
        throw new RMIException(response);
    }

    @Override
    public int compareTo(RMIClient o) {
        return Math.toIntExact(getResponseDelay() - o.getResponseDelay());
    }

    private long getResponseDelay() {
        return responseTime;
    }

    String who() {
        return serviceProxy.who();
    }
}
