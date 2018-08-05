package com.doodream.rmovjs.server;


import com.doodream.rmovjs.annotation.server.Controller;
import com.doodream.rmovjs.method.RMIMethod;
import com.doodream.rmovjs.model.Endpoint;
import com.doodream.rmovjs.model.RMIError;
import com.doodream.rmovjs.model.Request;
import com.doodream.rmovjs.model.Response;
import com.doodream.rmovjs.net.session.BlobSession;
import com.doodream.rmovjs.parameter.Param;
import com.doodream.rmovjs.serde.Converter;
import com.google.common.base.Preconditions;
import io.reactivex.Observable;
import io.reactivex.Single;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.util.*;

@AllArgsConstructor
@Builder
@NoArgsConstructor
@Data
public class RMIController {

    private static final Logger Log = LoggerFactory.getLogger(RMIController.class);
    private Controller controller;
    private Map<String, Endpoint> endpointMap;
    private Class stub;
    private Object impl;

    /**
     * create {@link RMIController} by analyzing fields of {@link RMIService}
     * @param field field of {@link RMIController}
     * @return created {@link RMIController}
     * @throws IllegalAccessException if no arg constructor is not aacessible
     * @throws InstantiationException fail to resolve implementation class of controller
     */
    static public RMIController create(Field field) throws IllegalAccessException, InstantiationException {
        return create(field, new Object[0]);
    }

    /**
     *
     * @param field
     * @param controllerImpls
     * @return
     * @throws IllegalAccessException
     * @throws InstantiationException
     */
    public static RMIController create(Field field, Object[] controllerImpls) throws IllegalAccessException, InstantiationException {

        Controller controller = field.getAnnotation(Controller.class);

        Preconditions.checkNotNull(controller, "controller should be annotated with @Controller");
        Class cls = field.getType();
        Class module = controller.module();


        Object impl = Observable.fromArray(controllerImpls)
                .filter(o -> isImplementOf(o, field.getGenericType()))
                .defaultIfEmpty(module.newInstance())
                .blockingFirst();

        Preconditions.checkNotNull(impl, "implementation should not null");

        Observable<Endpoint> endpointObservable = Observable.fromArray(cls.getDeclaredMethods())
                .filter(RMIMethod::isValidMethod)
                .map(method -> Endpoint.create(controller, method));

        Single<HashMap<String, Endpoint>> endpointLookupSingle = endpointObservable
                .collectInto(new HashMap<>(), RMIController::collectMethod);

        return Observable.just(RMIController.builder())
                .map(controllerBuilder -> controllerBuilder.impl(impl))
                .map(controllerBuilder -> controllerBuilder.controller(controller))
                .map(controllerBuilder -> controllerBuilder.stub(cls))
                .zipWith(endpointLookupSingle.toObservable(), RMIControllerBuilder::endpointMap)
                .map(RMIControllerBuilder::build)
                .blockingFirst();
    }


    /**
     * collect methods({@link Endpoint}) into map for lookup
     * @param map map to collect endpoint into
     * @param endpoint endpoint to be collected into the map
     */
    private static void collectMethod(HashMap<String, Endpoint> map, Endpoint endpoint) {
        map.put(endpoint.getUnique(), endpoint);
    }

    /**
     * check the controller is valid or not
     * @param field field
     * @return return true if the controller valid, otherwise return false
     */
    public static boolean isValidController(Field field) {
        Controller controller = field.getAnnotation(Controller.class);
        return controller != null;
    }


    private static boolean isImplementOf(Object o, Type itfcType) {
        return Arrays.asList(o.getClass().getGenericInterfaces()).contains(itfcType);
    }

    /**
     * return list of hash which uniquely mapped to any endpoints within controller
     * @return {@link List} of endpoints
     */
    List<String> getEndpoints() {
        return new ArrayList<>(endpointMap.keySet());
    }

    /**
     * handle client request and return response
     * @param request valid {@link Request} from client
     * @return {@link Response} for the request
     * @throws InvocationTargetException exception occurred within the method call
     * @throws IllegalAccessException 
     */
    Response handleRequest(Request request, Converter converter) throws InvocationTargetException, IllegalAccessException {

        Endpoint endpoint = endpointMap.get(request.getEndpoint());

        if(endpoint == null) {
            return Response.from(RMIError.NOT_FOUND);
        }

        Observable<Type> typeObservable = Observable.fromArray(endpoint.getJMethod().getGenericParameterTypes());

        List<Object> params = Observable.fromIterable(request.getParams())
                .sorted(Param::sort)
                .zipWith(typeObservable, (param, type) -> param.resolve(converter, type))
                .map(o -> {
                    if(o instanceof BlobSession) {
                        return request.getSession();
                    }
                    return o;
                })
                .toList().blockingGet();

        return (Response) endpoint.getJMethod().invoke(getImpl(), params.toArray());
    }
}
