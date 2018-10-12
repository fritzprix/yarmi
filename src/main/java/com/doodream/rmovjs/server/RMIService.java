package com.doodream.rmovjs.server;


import com.doodream.rmovjs.Properties;
import com.doodream.rmovjs.annotation.server.Service;
import com.doodream.rmovjs.model.*;
import com.doodream.rmovjs.net.ServiceAdapter;
import com.doodream.rmovjs.net.session.BlobSession;
import com.doodream.rmovjs.sdp.ServiceAdvertiser;
import com.doodream.rmovjs.serde.Converter;
import com.google.common.base.Preconditions;
import io.reactivex.Observable;
import io.reactivex.functions.BiConsumer;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * Created by innocentevil on 18. 5. 4.
 * generic request router for containing controller which handles request and returns response
 * , providing validity check for both request / response. it routes request message from client to the
 * targeting controller only for valid request and vice versa.
 *
 */
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class RMIService {

    private static Properties properties = new Properties();
    private static final Logger Log = LoggerFactory.getLogger(RMIService.class);

    private Service service;
    private HashMap<String, RMIController> controllerMap;
    private RMIServiceInfo serviceInfo;
    private ServiceAdapter adapter;
    private ServiceAdvertiser advertiser;
    private Converter converter;

    protected static final String TAG = RMIService.class.getCanonicalName();

    /**
     *
     * @param cls
     * @param advertiser
     * @param controllerImpls
     * @param <T>
     * @return
     */
    public static <T> RMIService create(Class<T> cls, ServiceAdvertiser advertiser, final Object ...controllerImpls) throws IllegalAccessException, InvocationTargetException, InstantiationException {

        Service service = cls.getAnnotation(Service.class);
        final String[] params = service.params();

        Constructor constructor = Observable.fromArray(service.adapter().getConstructors())
                .filter(new Predicate<Constructor<?>>() {
                    @Override
                    public boolean test(Constructor<?> constructor) throws Exception {
                        return constructor.getParameterCount() == params.length;
                    }
                })
                .blockingFirst();

        Preconditions.checkNotNull(constructor);

        ServiceAdapter adapter = (ServiceAdapter) constructor.newInstance(params);

        final RMIServiceInfo serviceInfo = RMIServiceInfo.builder()
                .name(service.name())
                .adapter(service.adapter())
                .negotiator(service.negotiator())
                .converter(service.converter())
                .params(Arrays.asList(params))
                .version(Properties.getVersionString())
                .build();

        final Converter converter = service.converter().newInstance();
        Preconditions.checkNotNull(converter, "converter is not declared");
        final RMIServiceBuilder builder = RMIService.builder();

        Observable<RMIController> basicControllerObservable = Observable.fromArray(BasicService.class.getDeclaredFields())
                .filter(new Predicate<Field>() {
                    @Override
                    public boolean test(Field field) throws Exception {
                        return RMIController.isValidController(field);
                    }
                })
                .map(new Function<Field, RMIController>() {
                    @Override
                    public RMIController apply(Field field) throws Exception {
                        return RMIController.create(field);
                    }
                })
                .cache();

        Observable<RMIController> controllerObservable = Observable.fromArray(cls.getDeclaredFields())
                .filter(new Predicate<Field>() {
                    @Override
                    public boolean test(Field field) throws Exception {
                        return RMIController.isValidController(field);
                    }
                })
                .map(new Function<Field, RMIController>() {
                    @Override
                    public RMIController apply(Field field) throws Exception {
                        return RMIController.create(field, controllerImpls);
                    }
                })
                .cache();

        controllerObservable
                .map(new Function<RMIController, ControllerInfo>() {
                    @Override
                    public ControllerInfo apply(RMIController rmiController) throws Exception {
                        return ControllerInfo.build(rmiController);
                    }
                })
                .toList()
                .doOnSuccess(new Consumer<List<ControllerInfo>>() {
                    @Override
                    public void accept(List<ControllerInfo> controllerInfos) throws Exception {
                        serviceInfo.setControllerInfos(controllerInfos);
                    }
                })
                .subscribe();

        controllerObservable = controllerObservable.mergeWith(basicControllerObservable);


        controllerObservable.collectInto(new HashMap<String, RMIController>(), new BiConsumer<HashMap<String, RMIController>, RMIController>() {
            @Override
            public void accept(HashMap<String, RMIController> stringRMIControllerHashMap, RMIController rmiController) throws Exception {
                RMIService.buildControllerMap(stringRMIControllerHashMap, rmiController);
            }
        }).doOnSuccess(new Consumer<HashMap<String, RMIController>>() {
            @Override
            public void accept(HashMap<String, RMIController> controllerMap) throws Exception {
                builder.controllerMap(controllerMap);
            }
        }).subscribe();


        return builder
                .adapter(adapter)
                .service(service)
                .advertiser(advertiser)
                .converter(converter)
                .serviceInfo(serviceInfo)
                .build();

    }

    /**
     *
     * @param cls service definition class
     * @param advertiser advertiser to be used service discovery
     * @return {@link RMIService} created from the service definition class
     * @throws IllegalAccessException constructor for components class is not accessible (e.g. no default constructor)
     * @throws InstantiationException fail to resolve components object (e.g. component is abstract class)
     * @throws InvocationTargetException exception caused at constructor of components
     */
    public static <T> RMIService create(Class<T> cls, ServiceAdvertiser advertiser) throws IllegalAccessException, InstantiationException, InvocationTargetException {
        Service service = cls.getAnnotation(Service.class);
        final String[] params = service.params();


        Constructor constructor = Observable.fromArray(service.adapter().getConstructors())
                .filter(new Predicate<Constructor<?>>() {
                    @Override
                    public boolean test(Constructor<?> constructor) throws Exception {
                        return constructor.getParameterCount() == params.length;
                    }
                })
                .blockingFirst();

        ServiceAdapter adapter = (ServiceAdapter) constructor.newInstance(((Object[]) params));
        final RMIServiceInfo serviceInfo = RMIServiceInfo.builder()
                .name(service.name())
                .adapter(service.adapter())
                .negotiator(service.negotiator())
                .converter(service.converter())
                .params(Arrays.asList(service.params()))
                .version(Properties.getVersionString())
                .build();

        final Converter converter = service.converter().newInstance();
        Preconditions.checkNotNull(converter, "converter is not declared");
        final RMIServiceBuilder builder = RMIService.builder();


        // register controller for BasicService
        Observable<RMIController> basicControllerObservable = Observable.fromArray(BasicService.class.getDeclaredFields())
                .filter(new Predicate<Field>() {
                    @Override
                    public boolean test(Field field) throws Exception {
                        return RMIController.isValidController(field);
                    }
                })
                .map(new Function<Field, RMIController>() {
                    @Override
                    public RMIController apply(Field field) throws Exception {
                        return RMIController.create(field);
                    }
                })
                .cache();

        Observable<RMIController> controllerObservable = Observable.fromArray(cls.getDeclaredFields())
                .filter(new Predicate<Field>() {
                    @Override
                    public boolean test(Field field) throws Exception {
                        return RMIController.isValidController(field);
                    }
                })
                .map(new Function<Field, RMIController>() {
                    @Override
                    public RMIController apply(Field field) throws Exception {
                        return RMIController.create(field);
                    }
                })
                .cache();

        controllerObservable
                .map(new Function<RMIController, ControllerInfo>() {
                    @Override
                    public ControllerInfo apply(RMIController rmiController) throws Exception {
                        return ControllerInfo.build(rmiController);
                    }
                })
                .toList()
                .doOnSuccess(new Consumer<List<ControllerInfo>>() {
                    @Override
                    public void accept(List<ControllerInfo> controllerInfos) throws Exception {
                        serviceInfo.setControllerInfos(controllerInfos);
                    }
                })
                .subscribe();

        controllerObservable = controllerObservable.mergeWith(basicControllerObservable);

        controllerObservable.collectInto(new HashMap<String, RMIController>(), new BiConsumer<HashMap<String, RMIController>, RMIController>() {
            @Override
            public void accept(HashMap<String, RMIController> stringRMIControllerHashMap, RMIController rmiController) throws Exception {
                RMIService.buildControllerMap(stringRMIControllerHashMap, rmiController);
            }
        }).doOnSuccess(new Consumer<HashMap<String, RMIController>>() {
            @Override
            public void accept(HashMap<String, RMIController> controllerMap) throws Exception {
                builder.controllerMap(controllerMap);
            }
        }).subscribe();


        return builder
                .adapter(adapter)
                .service(service)
                .advertiser(advertiser)
                .converter(converter)
                .serviceInfo(serviceInfo)
                .build();
    }

    /**
     * add controller into map which provides lookup for controller from endpoint hash
     * @param map map used to collect controller
     * @param controller controller to be collected
     */
    private static void buildControllerMap(final HashMap<String, RMIController> map, final RMIController controller) {
        Observable.fromIterable(controller.getEndpoints())
                .doOnNext(new Consumer<String>() {
                    @Override
                    public void accept(String s) throws Exception {
                        map.put(s, controller);
                    }
                })
                .subscribe();
    }

    /**
     * start to listen for client connection while advertising its service
     * @param block if true, this call will block indefinite time, otherwise return immediately
     * @throws IOException server 측 네트워크 endpoint 생성의 실패 혹은 I/O 오류
     * @throws IllegalAccessError the error thrown when {@link ServiceAdapter} fails to resolve dependency object (e.g. negotiator,
     * @throws InstantiationException if dependent class represents an abstract class,an interface, an array class, a primitive type, or void;or if the class has no nullary constructor;
     */
    public void listen(boolean block) throws IOException, IllegalAccessException, InstantiationException {
        serviceInfo.setProxyFactoryHint(adapter.listen(serviceInfo, converter, new Function<Request, Response>() {
            @Override
            public Response apply(Request request) throws Exception {
                return routeRequest(request);
            }
        }));
        advertiser.startAdvertiser(serviceInfo, block);
    }

    /**
     * route {@link Request} to target controller
     * @param request @{@link Request} from the client
     * @return {@link Response} for the given {@link Request}
     * @throws IllegalAccessException if this {@code Method} object is enforcing Java language access control and the underlying method is inaccessible.
     * @throws InvalidResponseException {@link Response} from controller is not valid, refer {@link Response::validate}
     * @throws IOException I/O error in sending response to client
     */
    private Response routeRequest(Request request) throws IllegalAccessException, InvalidResponseException, IOException {
        if(!Request.isValid(request)) {
            return end(Response.from(RMIError.BAD_REQUEST), request);
        }

        Response response;
        Preconditions.checkNotNull(request.getEndpoint());
        RMIController controller = controllerMap.get(request.getEndpoint());

        if(controller != null) {
            try {
                response = controller.handleRequest(request, converter);
                if (response == null) {
                    response = Response.from(RMIError.NOT_IMPLEMENTED);
                }
                return end(response, request);
            } catch (InvocationTargetException e) {
                Log.error("InvocationError : {}", e);
                return end(Response.from(RMIError.INTERNAL_SERVER_ERROR), request);
            }
        }

        return end(Response.from(RMIError.NOT_FOUND), request);
    }

    /**
     *
     * @param res {@link Response} from controller
     * @param req {@link Request} from client
     * @return validated response which contains request information
     * @throws InvalidResponseException the {@link Response} is not valid type, meaning controller logic has some problem
     * @throws IOException problem in closing session for the {@link Request}
     */
    private Response end(Response res, Request req) throws InvalidResponseException, IOException {
        res.setNonce(req.getNonce());
        final BlobSession session = req.getSession();
        if(session != null) {
            session.close();
        }
        try {
            Response.validate(res);
        } catch (RuntimeException e) {
            InvalidResponseException exception = new InvalidResponseException(String.format("Invalid response for endpoint : %s", req.getEndpoint()));
            exception.initCause(e);
            throw exception;
        }

        res.setEndpoint(req.getEndpoint());
        return res;
    }

    /**
     * stop service and release system resources
     * @throws IOException problem occurred in closing advertising channel
     */
    public void stop() throws IOException {
        advertiser.stopAdvertiser();
        adapter.close();
    }
}
