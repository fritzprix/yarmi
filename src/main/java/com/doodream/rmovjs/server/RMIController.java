package com.doodream.rmovjs.server;


import com.doodream.rmovjs.annotation.server.Controller;
import com.doodream.rmovjs.method.RMIMethod;
import com.doodream.rmovjs.model.Endpoint;
import io.reactivex.Observable;
import io.reactivex.Single;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.lang.reflect.Field;
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
                .map(nsdControllerBuilder -> nsdControllerBuilder.impl(impl))
                .map(nsdControllerBuilder -> nsdControllerBuilder.controller(controller))
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

    public List<String> getPaths() {
        return Observable.fromIterable(endpoints).toList().blockingGet();
    }
}
