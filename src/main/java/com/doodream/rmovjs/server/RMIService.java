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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashMap;

/**
 * Created by innocentevil on 18. 5. 4.
 */

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class RMIService {

    private static Properties properties = new Properties();
    private static final Logger Log = LogManager.getLogger(RMIService.class);

    private Service service;
    private HashMap<String, RMIController> controllerMap;
    private RMIServiceInfo serviceInfo;
    private ServiceAdapter adapter;
    private ServiceAdvertiser advertiser;
    private Converter converter;

    protected static final String TAG = RMIService.class.getCanonicalName();

    public static <T> RMIService create(Class<T> cls, ServiceAdvertiser advertiser) throws IllegalAccessException, InstantiationException, InvocationTargetException {
        Service service = cls.getAnnotation(Service.class);
        String[] params = service.params();


        Constructor constructor = Observable.fromArray(service.adapter().getConstructors())
                .filter(ctor -> ctor.getParameterCount() == params.length)
                .blockingFirst();

        ServiceAdapter adapter = (ServiceAdapter) constructor.newInstance(params);
        RMIServiceInfo serviceInfo = RMIServiceInfo.builder()
                .name(service.name())
                .adapter(service.adapter())
                .negotiator(service.negotiator())
                .converter(service.converter())
                .params(Arrays.asList(service.params()))
                .version(Properties.VERSION)
                .build();

        final Converter converter = (Converter) serviceInfo.getConverter().newInstance();
        Preconditions.checkNotNull(converter, "converter is not declared");
        RMIServiceBuilder builder = RMIService.builder();

        Observable<RMIController> controllerObservable = Observable.fromArray(cls.getDeclaredFields())
                .filter(RMIController::isValidController)
                .map(RMIController::create)
                .cache();

        controllerObservable.collectInto(new HashMap<>(), RMIService::buildControllerMap)
                .doOnSuccess(builder::controllerMap)
                .subscribe();

        controllerObservable
                .map(ControllerInfo::build)
                .toList()
                .doOnSuccess(serviceInfo::setControllerInfos)
                .subscribe();

        return builder
                .adapter(adapter)
                .service(service)
                .advertiser(advertiser)
                .converter(converter)
                .serviceInfo(serviceInfo)
                .build();
    }

    private static void buildControllerMap(HashMap<String, RMIController> map, RMIController controller) {
        Observable.fromIterable(controller.getEndpoints())
                .doOnNext(s -> map.put(s, controller))
                .subscribe();
    }


    public void listen(boolean block) throws Exception {
        serviceInfo.setProxyFactoryHint(adapter.listen(serviceInfo, converter, this::routeRequest));
        advertiser.startAdvertiser(serviceInfo, converter, block);
    }

    private Response routeRequest(Request request) throws InvocationTargetException, IllegalAccessException, InvalidResponseException, IOException {
        if(!Request.isValid(request)) {
            return end(Response.from(RMIError.BAD_REQUEST), request);
        }

        Response response;
        Preconditions.checkNotNull(request.getEndpoint());
        RMIController controller = controllerMap.get(request.getEndpoint());

        if(controller != null) {
            response = controller.handleRequest(request);
        } else {
            response = Response.from(RMIError.NOT_FOUND);
        }

        if(response == null) {
            response = Response.from(RMIError.NOT_IMPLEMENTED);
        }
        return end(response, request);
    }

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

    public void stop() throws Exception {
        advertiser.stopAdvertiser();
        adapter.close();
    }
}
