package com.doodream.rmovjs.client;

import com.doodream.rmovjs.annotation.server.Controller;
import com.doodream.rmovjs.annotation.server.Service;
import com.doodream.rmovjs.method.RMIMethod;
import com.doodream.rmovjs.model.Endpoint;
import com.doodream.rmovjs.net.RMIServiceProxy;
import io.reactivex.Observable;
import io.reactivex.Single;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class RMIClient {

    private String controllerPath;
    private Map<Method, Endpoint> methodMap;
    private WeakReference<Object> proxyWeakReference;
    private RMIServiceProxy serviceProxy;

    private static LinkedList<RMIClient> controllerClients = new LinkedList<>();

    public static <T> T create(RMIServiceProxy serviceProxy, Class svc, Class<T> ctrl) throws IllegalAccessException, InstantiationException, IOException {
        Service service = (Service) svc.getAnnotation(Service.class);
        assert service != null;

        Controller controller = Observable.fromArray(svc.getDeclaredFields())
                .filter(field -> field.getType().equals(ctrl))
                .map(field -> field.getAnnotation(Controller.class))
                .blockingFirst();

        assert controller != null;
        assert ctrl.isInterface();
        serviceProxy.open();

        RMIClientBuilder builder = RMIClient.builder();
        Observable<Method> methodObservable = Observable.fromArray(ctrl.getMethods())
                .filter(RMIMethod::isValidMethod);

        Observable<Endpoint> endpointObservable = methodObservable
                .map(method -> Endpoint.create(controller, method));

        Single<HashMap<Method, Endpoint>> hashMapSingle = methodObservable
                .zipWith(endpointObservable, RMIClient::zipIntoMethodMap)
                .collectInto(new HashMap<>(), RMIClient::collectMethodMap);

        builder.controllerPath(controller.path())
                .serviceProxy(serviceProxy);

        RMIClient rmiClient = builder
                .methodMap(hashMapSingle.blockingGet())
                .build();


        Object proxy = Proxy.newProxyInstance(ctrl.getClassLoader(), controller.module().getInterfaces(), rmiClient::handleInvocation);
        rmiClient.setProxyWeakReference(new WeakReference<>(proxy));

        // rmiClient has weakreference to proxy, and proxy has reference to rmiClient
        // so the proxy is no longer used (or referenced), it can be freed by garbage collector
        // and then, the rmiClient can be freed because only referencer which was proxy has been already freed

        return (T) proxy;
    }

    private static void collectMethodMap(Map<Method, Endpoint> into, Map<Method, Endpoint> methodEndpointMap) {
        into.putAll(methodEndpointMap);
    }

    private static Map<Method, Endpoint> zipIntoMethodMap(Method method, Endpoint endpoint) {
        Map<Method, Endpoint> methodEndpointMap = new HashMap<>();
        methodEndpointMap.put(method, endpoint);
        return methodEndpointMap;
    }


    private Object handleInvocation(Object proxy, Method method, Object[] objects) throws IOException {
        Endpoint endpoint = methodMap.get(method);
        if(endpoint == null) {
            return null;
        }
        endpoint.applyParam(objects);
        return serviceProxy.request(endpoint);
    }
}
