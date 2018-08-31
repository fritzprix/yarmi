package com.doodream.rmovjs.model;

import com.doodream.rmovjs.Properties;
import com.doodream.rmovjs.annotation.server.Controller;
import com.doodream.rmovjs.annotation.server.Service;
import com.doodream.rmovjs.server.RMIController;
import com.google.gson.annotations.SerializedName;
import io.reactivex.Observable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

@Builder
@EqualsAndHashCode(exclude = {"proxyFactoryHint"})
@Data
public class RMIServiceInfo {

    @SerializedName("name")
    private String name;

    @SerializedName("version")
    private String version;

    @SerializedName("adapter")
    private Class adapter;

    @SerializedName("negotiator")
    private Class negotiator;

    @SerializedName("converter")
    private Class converter;

    @SerializedName("params")
    private List<String> params;

    @SerializedName("interfaces")
    private List<ControllerInfo> controllerInfos;

    /**
     *  remoteHint is used to guess conntion information (like address or bluetooth device name etc.,)
     *
     */
    @SerializedName("hint")
    private String proxyFactoryHint;


    public static RMIServiceInfo from(Class<?> svc) {
        Service service = svc.getAnnotation(Service.class);
        final RMIServiceInfoBuilder builder = RMIServiceInfo.builder();

        builder.version(Properties.getVersionString())
                .adapter(service.adapter())
                .negotiator(service.negotiator())
                .converter(service.converter())
                .params(Arrays.asList(service.params()))
                .name(service.name());

        Observable.fromArray(svc.getDeclaredFields())
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
                        builder.controllerInfos(controllerInfos);
                    }
                })
                .subscribe();

        return builder.build();
    }

    public static boolean isComplete(RMIServiceInfo info) {
        return (info.getProxyFactoryHint() != null) &&
                (info.getControllerInfos() != null);
    }

    public void copyFrom(RMIServiceInfo info) {
        setProxyFactoryHint(info.getProxyFactoryHint());
        setParams(info.getParams());
        setAdapter(info.getAdapter());
        setControllerInfos(info.getControllerInfos());
        setName(info.getName());
        setNegotiator(info.getNegotiator());
        setVersion(info.getVersion());
    }
}
