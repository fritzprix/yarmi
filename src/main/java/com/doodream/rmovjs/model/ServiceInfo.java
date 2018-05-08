package com.doodream.rmovjs.model;

import com.doodream.rmovjs.Properties;
import com.doodream.rmovjs.annotation.server.Service;
import com.doodream.rmovjs.server.RMIController;
import com.doodream.rmovjs.server.RMIService;
import com.google.gson.annotations.SerializedName;
import io.reactivex.Observable;
import lombok.Builder;
import lombok.Data;

import java.util.HashMap;
import java.util.List;

@Builder
@Data
public class ServiceInfo {
    @SerializedName("name")
    private String name;

    // RMI Version
    @SerializedName("version")
    private String version;

    @SerializedName("interfaces")
    private List<ControllerInfo> controllerInfos;

    public static <T> ServiceInfo from(Class<T> svc) {
        Service service = svc.getAnnotation(Service.class);
        ServiceInfoBuilder builder = ServiceInfo.builder();

        builder.version(Properties.VERSION)
                .name(service.name());

        Observable.fromArray(svc.getDeclaredFields())
                .filter(RMIController::isValidController)
                .map(RMIController::create)
                .map(ControllerInfo::build)
                .toList()
                .doOnSuccess(builder::controllerInfos)
                .subscribe();

        return builder.build();

    }
}
