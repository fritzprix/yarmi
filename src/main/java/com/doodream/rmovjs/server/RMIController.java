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

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

@AllArgsConstructor
@Builder
@NoArgsConstructor
@Data
public class RMIController {

    private Controller controller;
    private Map<RMIMethod, Map<String, Endpoint>> endpointMap;
    private Set<String> endpoints;
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

        Single<TreeMap<RMIMethod, Map<String, Endpoint>>> collection = endpointsObservable
                .collectInto(new TreeMap<>(), RMIController::collectMethod);

        Single<Set<String>> pathCollection = endpointsObservable
                .collectInto(new HashSet<>(), RMIController::collectPaths);

        return Observable.just(RMIController.builder())
                .map(controllerBuilder -> controllerBuilder.impl(impl))
                .map(controllerBuilder -> controllerBuilder.controller(controller))
                .map(controllerBuilder -> controllerBuilder.itfcCls(cls))
                .zipWith(collection.toObservable(), RMIControllerBuilder::endpointMap)
                .zipWith(pathCollection.toObservable(), RMIControllerBuilder::endpoints)
                .map(RMIControllerBuilder::build)
                .blockingFirst();
    }

    private static void collectPaths(Set<String> strings, Endpoint endpoint) {
        strings.add(endpoint.getPath());
    }

    private static void collectMethod(TreeMap<RMIMethod, Map<String, Endpoint>> map, Endpoint endpoint) {
        Map<String, Endpoint> pathToEpMap = map.get(endpoint.getMethod());
        if(pathToEpMap == null) {
            pathToEpMap = new TreeMap<>();
        }
        pathToEpMap.put(endpoint.getPath(), endpoint);
        map.put(endpoint.getMethod(), pathToEpMap);
    }


    public static boolean isValidController(Field field) {
        return field.getAnnotation(Controller.class) != null;
    }

    List<String> getPaths() {
        return Observable.fromIterable(endpoints).toList().blockingGet();
    }

    Response handleRequest(Request request) throws InvocationTargetException, IllegalAccessException {
        final Map<String, Endpoint> pathMap = endpointMap.get(request.getMethodType());
        if(pathMap == null) {
            return RMIError.NOT_FOUND.getResponse(request);
        }
        final Endpoint endpoint = pathMap.get(request.getPath());
        if(endpoint == null) {
            return RMIError.NOT_FOUND.getResponse(request);
        }
        List<Object> params = Observable.fromIterable(request.getParameters())
                .sorted(Param::sort)
                .map(Param::instantiate)
                .toList().blockingGet();

        Response response = (Response) endpoint.getJMethod().invoke(getImpl(), params.toArray());
        return request.answerWith(response);
    }

}
