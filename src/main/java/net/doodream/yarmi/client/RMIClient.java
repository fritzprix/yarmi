package net.doodream.yarmi.client;

import net.doodream.yarmi.annotation.RMIException;
import net.doodream.yarmi.annotation.server.Controller;
import net.doodream.yarmi.annotation.server.Service;
import net.doodream.yarmi.data.Endpoint;
import net.doodream.yarmi.data.RMIError;
import net.doodream.yarmi.data.RMIServiceInfo;
import net.doodream.yarmi.data.Response;
import net.doodream.yarmi.method.RMIMethod;
import net.doodream.yarmi.net.ServiceProxy;
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

    static RMIClient createClient(ServiceProxy serviceProxy, Class<?> svc, Class<?>[] ctrl, long timeoutInMills) throws IllegalArgumentException {
        if(svc == null) {
            throw new IllegalArgumentException("null service");
        }
        Service service = svc.getAnnotation(Service.class);
        if(service == null) {
            throw new IllegalArgumentException(String.format(Locale.ENGLISH, "no service annotation on %s", svc));
        }
        final RMIServiceInfo serviceInfo = RMIServiceInfo.from(svc);
        final ConcurrentHashMap<Class<?>, Controller> controllerMap  = new ConcurrentHashMap<>();

        final Set<Field> controllers = getController(svc, serviceProxy);
        if(ctrl == null) {
            // all the controllers declared are added to call proxy, if ctrl is given as null
            for (Field controller : controllers) {
                controllerMap.put(controller.getType(), controller.getAnnotation(Controller.class));
            }
        } else {
            Set<Class<?>> controllerSet = new HashSet<>(Arrays.asList(ctrl));
            for (Field controller : controllers) {
                if(controllerSet.contains(controller.getType())) {
                    controllerMap.put(controller.getType(), controller.getAnnotation(Controller.class));
                }
            }
        }
        if(controllerMap.isEmpty()) {
            // throw IllegalArgumentException if there is no matched controller
            throw new IllegalArgumentException(String.format(Locale.ENGLISH, "no valid controllers for %s", service.name()));
        }

        Set<Method> validMethods = new HashSet<>();
        for (Map.Entry<Class<?>, Controller> entry : controllerMap.entrySet()) {
            Set<Method> methods = extractRMIMethods(entry.getKey());
            validMethods.addAll(methods);
        }

        if(validMethods.isEmpty()) {
            throw new IllegalArgumentException("no valid method");
        }


        try {
            if(serviceProxy.open()) {
                Log.debug("{} is opened", serviceProxy.who());
            }

            RMIClient rmiClient = new RMIClient(serviceProxy, timeoutInMills);
            final HashMap<Method, Endpoint> endpointMap = new HashMap<>();
            for (Method validMethod : validMethods) {
                final Endpoint endpoint = Endpoint.create(controllerMap.get(validMethod.getDeclaringClass()), validMethod);
                endpointMap.put(validMethod, endpoint);
            }

            rmiClient.setMethodEndpointMap(endpointMap);
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

    private static Set<Field> getController(Class<?> svc, ServiceProxy serviceProxy) {
        final Set<Field> result = new HashSet<>();
        for (Field field : svc.getDeclaredFields()) {
            if(serviceProxy.provide(field.getType()) && isAnnotatedWithController(field)) {
                result.add(field);
            }
        }
        return result;
    }

    private static Set<Method> extractRMIMethods(Class<?> cls) {
        Set<Method> methods = new HashSet<>();
        for (Method method : cls.getMethods()) {
            if(RMIMethod.isValidMethod(method)) {
                methods.add(method);
            }
        }
        return methods;
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
