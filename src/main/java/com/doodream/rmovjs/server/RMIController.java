package com.doodream.rmovjs.server;


import com.doodream.rmovjs.annotation.server.Controller;
import com.doodream.rmovjs.method.RMIMethod;
import com.doodream.rmovjs.model.Endpoint;
import com.doodream.rmovjs.model.RMIError;
import com.doodream.rmovjs.model.Request;
import com.doodream.rmovjs.model.Response;
import com.doodream.rmovjs.parameter.Param;
import io.reactivex.Observable;
import io.reactivex.Single;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.util.*;

@AllArgsConstructor
@Builder
@NoArgsConstructor
@Data
public class RMIController {

    private static final Logger Log = LogManager.getLogger(RMIController.class);
    private Controller controller;
    private Map<String, Endpoint> endpointMap;
    private Object impl;
    private Class itfcCls;

    static public RMIController create(Field field) throws IllegalAccessException, InstantiationException {
        Controller controller = field.getAnnotation(Controller.class);
        Class cls = field.getType();
        assert controller != null;
        Class module = controller.module();
        Object impl = module.newInstance();

        Observable<Endpoint> endpointsObservable = Observable.fromArray(cls.getMethods())
                .filter(RMIMethod::isValidMethod)
                .map(method -> Endpoint.create(controller, method));

        Single<HashMap<String, Endpoint>> endpointLookupSingle = endpointsObservable
                .collectInto(new HashMap<>(), RMIController::collectMethod);

        return Observable.just(RMIController.builder())
                .map(controllerBuilder -> controllerBuilder.impl(impl))
                .map(controllerBuilder -> controllerBuilder.controller(controller))
                .map(controllerBuilder -> controllerBuilder.itfcCls(cls))
                .zipWith(endpointLookupSingle.toObservable(), RMIControllerBuilder::endpointMap)
                .map(RMIControllerBuilder::build)
                .blockingFirst();
    }

    private static void collectMethod(HashMap<String, Endpoint> map, Endpoint endpoint) {
        map.put(endpoint.getUnique(), endpoint);
    }

    public static boolean isValidController(Field field) {
        return field.getAnnotation(Controller.class) != null;
    }

    public List<String> getEndpoints() {
        return new ArrayList<>(endpointMap.keySet());
    }

    Response handleRequest(Request request) throws InvocationTargetException, IllegalAccessException {

        Endpoint endpoint = endpointMap.get(request.getEndpoint());
        Log.debug("Endpoint Lookup : {} => {}", request.getEndpoint(), endpoint);

        if(endpoint == null) {
            return Response.from(RMIError.NOT_FOUND);
        }

        Observable<Type> typeObservable = Observable.fromArray(endpoint.getJMethod().getGenericParameterTypes());

        List<Object> params = Observable.fromIterable(request.getParams())
                .sorted(Param::sort)
                .zipWith(typeObservable, Param::instantiate)
                .toList().blockingGet();

        return (Response) endpoint.getJMethod().invoke(getImpl(), params.toArray());
    }
}
