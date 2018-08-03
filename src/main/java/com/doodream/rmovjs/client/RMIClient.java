package com.doodream.rmovjs.client;

import com.doodream.rmovjs.annotation.server.Controller;
import com.doodream.rmovjs.annotation.server.Service;
import com.doodream.rmovjs.method.RMIMethod;
import com.doodream.rmovjs.model.Endpoint;
import com.doodream.rmovjs.model.RMIServiceInfo;
import com.doodream.rmovjs.model.Response;
import com.doodream.rmovjs.net.RMIServiceProxy;
import com.google.common.base.Preconditions;
import io.reactivex.Observable;
import io.reactivex.Single;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *  {@link RMIClient} build method invocation proxy from {@link RMIServiceProxy} which is discovered from SDP
 *
 */
public class RMIClient implements InvocationHandler  {


    private static final Logger Log = LoggerFactory.getLogger(RMIClient.class);

    private Map<Method, Endpoint> methodMap;
    private RMIServiceProxy serviceProxy;

    private RMIClient(RMIServiceProxy serviceProxy) {
        this.serviceProxy = serviceProxy;
    }

    private void setMethodEndpointMap(Map<Method, Endpoint> map) {
        this.methodMap = map;
    }


    /**
     * close method invocation proxy created by {@link #create(RMIServiceProxy, Class, Class)} or {@link #createService(RMIServiceProxy, Class)} method
     * @param proxy returned proxy instance from {@link #create(RMIServiceProxy, Class, Class)} or {@link #createService(RMIServiceProxy, Class)}
     * @throws IOException fail to close network connection for service proxy
     */
    public static void destroy(Object proxy) throws IOException {
        Class proxyClass = proxy.getClass();
        if(Proxy.isProxyClass(proxyClass)) {
            RMIClient client = (RMIClient) Proxy.getInvocationHandler(proxy);
            client.close();
        } else {
            Service service = proxy.getClass().getAnnotation(Service.class);
            if(service == null) {
                throw new IllegalArgumentException("Invalid Proxy");
            }
            Observable.fromArray(proxyClass.getDeclaredFields())
                    .filter(field -> field.getAnnotation(Controller.class) != null)
                    .map(field -> field.get(proxy))
                    .blockingSubscribe(RMIClient::destroy);
        }
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
    public static <T> T createService(RMIServiceProxy serviceProxy, Class<T> svc) throws IllegalAccessException, InstantiationException {
        Object svcProxy = svc.newInstance();

        Observable.fromArray(svc.getDeclaredFields())
                .filter(field -> field.getAnnotation(Controller.class) != null)
                .blockingSubscribe(field -> {
                    Object controller = create(serviceProxy, svc, field.getType());
                    field.setAccessible(true);
                    field.set(svcProxy, controller);
                });

        return (T) svcProxy;
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
    public static <T> T create(RMIServiceProxy serviceProxy, Class svc, Class<T> ctrl) {
        Service service = (Service) svc.getAnnotation(Service.class);
        Preconditions.checkNotNull(service);
        if(!serviceProxy.provide(ctrl)) {
            return null;
        }

        Controller controller = Observable.fromArray(svc.getDeclaredFields())
                .filter(field -> field.getType().equals(ctrl))
                .map(field -> field.getAnnotation(Controller.class))
                .blockingFirst(null);

        final RMIServiceInfo serviceInfo = RMIServiceInfo.from(svc);
        List<Method> validMethods = Observable.fromArray(ctrl.getMethods())
                .filter(RMIMethod::isValidMethod).toList().blockingGet();


        Preconditions.checkNotNull(controller, "no matched controller");
        Preconditions.checkArgument(ctrl.isInterface());
        Preconditions.checkNotNull(serviceInfo, "Invalid Service Class %s", svc);
        Preconditions.checkArgument(validMethods.size() > 0);

        try {
            if (!serviceProxy.isOpen()) {
                // RMIServiceProxy is opened only once
                serviceProxy.open();
            }

            RMIClient rmiClient = new RMIClient(serviceProxy);

            Observable<Endpoint> endpointObservable = Observable.fromIterable(validMethods)
                    .map(method -> Endpoint.create(controller, method));

            Single<HashMap<Method, Endpoint>> hashMapSingle = Observable.fromIterable(validMethods)
                    .zipWith(endpointObservable, RMIClient::zipIntoMethodMap)
                    .collectInto(new HashMap<>(), RMIClient::collectMethodMap);

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

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        serviceProxy.close();
    }

    /**
     * collect maps between {@link Method} and @{@link Endpoint} into single map
     * @param into map collecting fragmented (or partial) map of {@link Method} and {@link Endpoint}
     * @param methodEndpointMap fragmented (or partial) map of {@link Method} and {@link Endpoint}
     */
    private static void collectMethodMap(Map<Method, Endpoint> into, Map<Method, Endpoint> methodEndpointMap) {
        into.putAll(methodEndpointMap);
    }

    /**
     *
     * @param method
     * @param endpoint
     * @return
     */
    private static Map<Method, Endpoint> zipIntoMethodMap(Method method, Endpoint endpoint) {
        Map<Method, Endpoint> methodEndpointMap = new HashMap<>();
        methodEndpointMap.put(method, endpoint);
        return methodEndpointMap;
    }

    /**
     * close service proxy used by this call proxy
     * @throws IOException
     */
    private void close() throws IOException {
        serviceProxy.close();
    }



    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Endpoint endpoint = methodMap.get(method);
        if(endpoint == null) {
            return null;
        }
        try {
            return serviceProxy.request(endpoint, args);
        } catch (IOException e) {
            serviceProxy.close();
            return Response.error(-1, e.getLocalizedMessage());
        }
    }
}
