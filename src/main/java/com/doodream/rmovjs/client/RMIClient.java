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
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
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


    public static void destroy(Object proxy) throws IOException {
        Class proxyClass = proxy.getClass();
        if(Proxy.isProxyClass(proxyClass)) {
            RMIClient client = (RMIClient) Proxy.getInvocationHandler(proxy);
            client.close();
        } else {
            Service service = proxy.getClass().getAnnotation(Service.class);
            if(service == null) {
                throw new RuntimeException("Invalid Proxy");
            }
            Observable.fromArray(proxyClass.getDeclaredFields())
                    .filter(field -> field.getAnnotation(Controller.class) != null)
                    .map(field -> field.get(proxy))
                    .blockingSubscribe(RMIClient::destroy);
        }
    }


    public static <T> T createService(RMIServiceProxy serviceProxy, Class<T> svc) throws IllegalAccessException, InstantiationException {
        Object svcProxy = svc.newInstance();

        Observable.fromArray(svc.getDeclaredFields())
                .filter(field -> field.getAnnotation(Controller.class) != null)
                .blockingSubscribe(field -> {
                    Object controller = create(serviceProxy, svc, field.getType());
                    field.set(svcProxy, controller);
                });

        return (T) svcProxy;
    }

    /**
     *
     * @param serviceProxy
     * @param svc
     * @param ctrl
     * @param <T>
     * @return
     */
    @Nullable
    public static <T> T create(RMIServiceProxy serviceProxy, Class svc, Class<T> ctrl) {
        Service service = (Service) svc.getAnnotation(Service.class);
        if(!serviceProxy.provide(ctrl)) {
            return null;
        }


        try {
            Preconditions.checkNotNull(service);
            Controller controller = Observable.fromArray(svc.getDeclaredFields())
                    .filter(field -> field.getType().equals(ctrl))
                    .map(field -> field.getAnnotation(Controller.class))
                    .blockingFirst(null);

            Preconditions.checkNotNull(controller, "no matched controller");
            Preconditions.checkArgument(ctrl.isInterface());


            final RMIServiceInfo serviceInfo = RMIServiceInfo.from(svc);
            Preconditions.checkNotNull(serviceInfo, "Invalid Service Class %s", svc);


            if (!serviceProxy.isOpen()) {
                serviceProxy.open();
            }

            RMIClient rmiClient = new RMIClient(serviceProxy);


            Observable<Method> methodObservable = Observable.fromArray(ctrl.getMethods())
                    .filter(RMIMethod::isValidMethod);

            Observable<Endpoint> endpointObservable = methodObservable
                    .map(method -> Endpoint.create(controller, method));

            Single<HashMap<Method, Endpoint>> hashMapSingle = methodObservable
                    .zipWith(endpointObservable, RMIClient::zipIntoMethodMap)
                    .collectInto(new HashMap<>(), RMIClient::collectMethodMap);

            rmiClient.setMethodEndpointMap(hashMapSingle.blockingGet());

            // TODO: 18. 7. 31 consider give all the available controller interface to the call proxy
            // main concern is...
            // what happen if there are two methods declared in different interfaces which is identical in parameter & return type, etc.
            // TODO: 18. 7. 31 method collision is not properly handled at the moment, simple poc is performed to test
            // https://gist.github.com/fritzprix/ca0ecc08fc3125cde529dd11185be0b9

            Object proxy = Proxy.newProxyInstance(ctrl.getClassLoader(), new Class[]{ctrl }, rmiClient);

            Log.debug("service proxy is created");
            return (T) proxy;
        } catch (Exception e) {
            Log.error("{}", e);
            return null;
        }
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        serviceProxy.close();
    }

    /**
     *
     * @param into
     * @param methodEndpointMap
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
