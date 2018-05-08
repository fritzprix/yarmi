package com.doodream.rmovjs.server;


import com.doodream.rmovjs.Properties;
import com.doodream.rmovjs.annotation.server.Service;
import com.doodream.rmovjs.model.*;
import com.doodream.rmovjs.net.ServiceAdapter;
import com.doodream.rmovjs.sdp.ServiceAdvertiser;
import io.reactivex.Observable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
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
    private ServiceInfo serviceInfo;
    private ServiceAdapter adapter;
    private ServiceAdvertiser advertiser;

    protected static final String TAG = RMIService.class.getCanonicalName();

    public static <T> RMIService create(Class<T> cls, ServiceAdvertiser advertiser) throws IllegalAccessException, InstantiationException {

        Properties.load();
        Service service = cls.getAnnotation(Service.class);

        ServiceAdapter adapter = service.adapter().newInstance();
        ServiceInfo serviceInfo = ServiceInfo.builder()
                .name(service.name())
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


    public void listen() throws Exception {
        adapter.listen(serviceInfo, this::routeRequest);
        advertiser.startAdvertiser(serviceInfo);
    }

    private Response routeRequest(Request request) throws InvocationTargetException, IllegalAccessException {
        final String path = request.getPath();
        assert path != null;
        RMIController controller = controllerMap.get(request.getPath());
        if(controller != null) {
            return controller.handleRequest(request);
        }
        return RMIError.NOT_FOUND.getResponse(request);
    }

    public void stop() throws Exception {
        advertiser.stopAdvertiser();
        adapter.close();
    }
}
