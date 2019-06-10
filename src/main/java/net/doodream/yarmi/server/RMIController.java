package net.doodream.yarmi.server;


import net.doodream.yarmi.annotation.server.Controller;
import net.doodream.yarmi.method.RMIMethod;
import net.doodream.yarmi.model.Endpoint;
import net.doodream.yarmi.model.RMIError;
import net.doodream.yarmi.model.Request;
import net.doodream.yarmi.model.Response;
import net.doodream.yarmi.net.session.BlobSession;
import net.doodream.yarmi.parameter.Param;
import net.doodream.yarmi.serde.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.*;

public class RMIController {

    private static final Logger Log = LoggerFactory.getLogger(RMIController.class);
    private Controller controller;
    private Map<String, Endpoint> endpointMap;
    private Class stub;
    private Object impl;

    static class Builder {
        private final RMIController controller = new RMIController();

        public Builder impl(Object impl) {
            controller.impl = impl;
            return this;
        }

        public Builder controller(Controller controller) {
            this.controller.controller = controller;
            return this;
        }

        public Builder stub(Class cls) {
            controller.stub = cls;
            return this;
        }

        public Builder endpointMap(HashMap<String, Endpoint> endpointMap) {
            controller.endpointMap = endpointMap;
            return this;
        }

        public RMIController build() {
            return controller;
        }
    }

    static Builder builder() {
        return new Builder();
    }


    /**
     * create {@link RMIController} by analyzing fields of {@link RMIService}
     * @param field field of {@link RMIController}
     * @return created {@link RMIController}
     * @throws IllegalAccessException if no arg constructor is not aacessible
     * @throws InstantiationException fail to resolve implementation class of controller
     */
    public static RMIController create(Field field) throws IllegalAccessException, InstantiationException {
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
    public static RMIController create(final Field field, Object[] controllerImpls) throws IllegalAccessException, InstantiationException {

        final Controller controller = field.getAnnotation(Controller.class);
        final Class cls = field.getType();
        Class module = controller.module();


        Object impl = getValidImpl(controllerImpls, field.getGenericType());
        if(impl == null) {
            impl = module.newInstance();
        }
        final Object validImpl = impl;
        final HashMap<String, Endpoint> endpointMap = new HashMap<>();

        for (Method method : cls.getDeclaredMethods()) {
            if(!RMIMethod.isValidMethod(method)) {
                continue;
            }

            final Endpoint endpoint = Endpoint.create(controller, method);
            RMIController.collectMethod(endpointMap, endpoint);
        }
//        Observable<Endpoint> endpointObservable = Observable.fromArray(cls.getDeclaredMethods())
//                .filter(new Predicate<Method>() {
//                    @Override
//                    public boolean test(Method method) throws Exception {
//                        return RMIMethod.isValidMethod(method);
//                    }
//                })
//                .map(new Function<Method, Endpoint>() {
//                    @Override
//                    public Endpoint apply(Method method) throws Exception {
//                        return Endpoint.create(controller, method);
//                    }
//                });
//
//        Single<HashMap<String, Endpoint>> endpointLookupSingle = endpointObservable
//                .collectInto(new HashMap<>(), (stringEndpointHashMap, endpoint) -> RMIController.collectMethod(stringEndpointHashMap, endpoint));

        return RMIController.builder()
                .impl(validImpl)
                .controller(controller)
                .endpointMap(endpointMap)
                .stub(cls)
                .build();

//        return Observable.just(RMIController.builder())
//                .map(new Function<Builder, Builder>() {
//                    @Override
//                    public Builder apply(Builder builder) throws Exception {
//                        return builder.impl(validImpl).controller(controller).stub(cls);
//                    }
//                })
//                .zipWith(endpointLookupSingle.toObservable(), new BiFunction<Builder, HashMap<String, Endpoint>, Builder>() {
//                    @Override
//                    public Builder apply(Builder builder, HashMap<String, Endpoint> stringEndpointHashMap) throws Exception {
//                        return builder.endpointMap(stringEndpointHashMap);
//                    }
//                })
//                .map(new Function<Builder, RMIController>() {
//
//                    @Override
//                    public RMIController apply(Builder builder) throws Exception {
//                        return builder.build();
//                    }
//                })
//                .blockingFirst();

    }

    private static Object getValidImpl(Object[] controllerImpls, Type genericType) {
        for (Object impl : controllerImpls) {
            if(isImplementOf(impl, genericType)) {
                return impl;
            }
        }
        return null;
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


    private RMIController() { }

    public Class getStub() {
        return stub;
    }

    public Controller getController() {
        return controller;
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
    Response handleRequest(final Request request, final Converter converter) throws InvocationTargetException, IllegalAccessException {

        Endpoint endpoint = endpointMap.get(request.getEndpoint());

        if(endpoint == null) {
            return Response.from(RMIError.NOT_FOUND);
        }

        final Type[] types = endpoint.getJMethod().getGenericParameterTypes();
        final List<Param> unresolvedParams = request.getParams();
        final List<Object> resolvedParams = new ArrayList<>();
        unresolvedParams.sort((o1, o2) -> Param.sort(o1, o2));

        for (int i = 0; i < unresolvedParams.size(); i++) {
            final Param param = unresolvedParams.get(i);
            Object resolved;
            try {
                resolved = param.resolve(converter, types[i]);
            } catch (InstantiationException | ClassNotFoundException e) {
                continue;
            }
            if(resolved instanceof BlobSession) {
                resolved = request.getSession();
            }
            resolvedParams.add(resolved);
        }

        Log.trace("invoke request handler {} for ({})", endpoint.getJMethod().getName(), request.getNonce());
        return (Response) endpoint.getJMethod().invoke(impl, resolvedParams.toArray(new Object[0]));
    }
}
