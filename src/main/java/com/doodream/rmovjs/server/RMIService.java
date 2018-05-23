package com.doodream.rmovjs.server;


import com.doodream.rmovjs.Properties;
import com.doodream.rmovjs.annotation.server.Service;
import com.doodream.rmovjs.model.*;
import com.doodream.rmovjs.net.ServiceAdapter;
import com.doodream.rmovjs.sdp.ServiceAdvertiser;
import com.google.common.base.Preconditions;
import io.reactivex.Observable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * Created by innocentevil on 18. 5. 4.
 */

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class RMIService {

    private static Properties properties = new Properties();

    private Service service;
    private HashMap<String, RMIController> controllerMap;
    private RMIServiceInfo serviceInfo;
    private ServiceAdapter adapter;
    private ServiceAdvertiser advertiser;

    protected static final String TAG = RMIService.class.getCanonicalName();

    public static <T> RMIService create(Class<T> cls, ServiceAdvertiser advertiser) throws IllegalAccessException, InstantiationException, InvocationTargetException {

        Service service = cls.getAnnotation(Service.class);
        String[] params = service.params();


        Constructor constructor = Observable.fromArray(service.adapter().getConstructors())
                .filter(cstr -> cstr.getParameterCount() == params.length)
                .blockingFirst();

        ServiceAdapter adapter = (ServiceAdapter) constructor.newInstance(params);
        RMIServiceInfo serviceInfo = RMIServiceInfo.builder()
                .name(service.name())
                .adapter(service.adapter())
                .negotiator(service.negotiator())
                .params(Arrays.asList(service.params()))
                .version(Properties.VERSION)
                .build();


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
                .serviceInfo(serviceInfo)
                .build();
    }

    private static void buildControllerMap(HashMap<String, RMIController> map, RMIController controller) {
        List<String> paths = controller.getPaths();
        Observable.fromIterable(paths)
                .doOnNext(s -> map.put(s, controller))
                .subscribe();
    }


    public void listen(boolean block) throws Exception {
        serviceInfo.setProxyFactoryHint(adapter.listen(serviceInfo, this::routeRequest));
        advertiser.startAdvertiser(serviceInfo, block);
    }

    private Response routeRequest(Request request) throws InvocationTargetException, IllegalAccessException {
        final String path = request.getPath();
        Response response;

        if(request.isValid()) {
            response = Response.from(RMIError.BAD_REQUEST);
        }
        Preconditions.checkNotNull(path);
        RMIController controller = controllerMap.get(request.getPath());

        if(controller != null) {
            response = controller.handleRequest(request);
        } else {
            response = Response.from(RMIError.NOT_FOUND);
        }
        if(response == null) {
            response = Response.from(RMIError.NOT_IMPLEMENTED);
        }
        Preconditions.checkNotNull(response);
        response.setEndpoint(request.getEndpoint());
        return response;
    }

    public void stop() throws Exception {
        advertiser.stopAdvertiser();
        adapter.close();
    }
}
