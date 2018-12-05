package com.doodream.rmovjs.model;

import com.doodream.rmovjs.Properties;
import com.doodream.rmovjs.annotation.server.Service;
import com.doodream.rmovjs.net.ServiceProxy;
import com.doodream.rmovjs.net.ServiceAdapter;
import com.doodream.rmovjs.server.RMIController;
import com.google.gson.annotations.SerializedName;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.functions.Function;
import lombok.*;

import java.util.Arrays;
import java.util.List;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"proxyFactoryHint", "alias"})
@Data
public class RMIServiceInfo {

    /**
     * unique name of service
     */
    @SerializedName("name")
    private String name;

    /**
     * unique name of the service provider
     */
    @SerializedName("provider")
    private String provider;

    /**
     * user defined alias for the service
     */
    @SerializedName("alias")
    private String alias;

    /**
     * interface version
     */
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
     *  remoteHint is used to guess connection information (like address or bluetooth device name etc.,)
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
                .provider(service.provider())
                .name(service.name());

        Observable.fromArray(svc.getDeclaredFields())
                .filter(field -> RMIController.isValidController(field))
                .map(field -> RMIController.create(field))
                .map(rmiController -> ControllerInfo.build(rmiController))
                .toList()
                .doOnSuccess(controllerInfos -> builder.controllerInfos(controllerInfos))
                .subscribe();

        return builder.build();
    }

    public static boolean isValid(RMIServiceInfo info) {
        return (info.getProxyFactoryHint() != null) &&
                (info.getControllerInfos() != null);
    }

    public static ServiceProxy toServiceProxy(final RMIServiceInfo info) {
        return Single.just(info)
                .map((Function<RMIServiceInfo, Class<?>>) rmiServiceInfo -> rmiServiceInfo.getAdapter())
                .map((Function<Class<?>, Object>) cls -> cls.newInstance())
                .cast(ServiceAdapter.class)
                .map(serviceAdapter -> serviceAdapter.getProxyFactory(info))
                .map(serviceProxyFactory -> serviceProxyFactory.build())
                .onErrorReturn(throwable -> ServiceProxy.NULL_PROXY)
                .filter(proxy -> !ServiceProxy.NULL_PROXY.equals(proxy))
                .blockingGet(ServiceProxy.NULL_PROXY);
    }

    public void copyFrom(RMIServiceInfo info) {
        setProxyFactoryHint(info.getProxyFactoryHint());
        setParams(info.getParams());
        setAdapter(info.getAdapter());
        setControllerInfos(info.getControllerInfos());
        setName(info.getName());
        setAlias(info.getAlias());
        setNegotiator(info.getNegotiator());
        setProvider(info.getProvider());
        setVersion(info.getVersion());
    }
}
