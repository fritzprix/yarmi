package com.doodream.rmovjs.server;


import com.doodream.rmovjs.Properties;
import com.doodream.rmovjs.annotation.server.Service;
import com.doodream.rmovjs.model.Request;
import com.doodream.rmovjs.model.Response;
import com.doodream.rmovjs.model.ServiceInfo;
import com.doodream.rmovjs.net.ServiceAdapter;
import io.reactivex.Observable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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

    protected static final String TAG = RMIService.class.getCanonicalName();

    public static <T> RMIService create(Class<T> cls) throws IllegalAccessException, InstantiationException {

        Properties.load();
        Service service = cls.getAnnotation(Service.class);

        ServiceAdapter adapter = service.adapter().newInstance();
        ServiceInfo serviceInfo = ServiceInfo.builder()
                .name(service.name())
                .version(Properties.VERSION)
                .build();


        RMIServiceBuilder builder = RMIService.builder();


        Observable.fromArray(cls.getDeclaredFields())
                .filter(RMIController::isValidController)
                .map(RMIController::create)
                .collectInto(new HashMap<>(), RMIService::buildControllerMap)
                .doOnSuccess(builder::controllerMap)
                .subscribe();

        return builder
                .adapter(adapter)
                .service(service)
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
    }

    private Response routeRequest(Request request) {
        System.out.println(request);
        return null;
    }

    public void stop() throws Exception {
        adapter.close();
    }
}
