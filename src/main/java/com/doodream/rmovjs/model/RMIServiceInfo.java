package com.doodream.rmovjs.model;

import com.doodream.rmovjs.Properties;
import com.doodream.rmovjs.annotation.server.Service;
import com.doodream.rmovjs.net.Negotiator;
import com.doodream.rmovjs.net.ServiceAdapter;
import com.doodream.rmovjs.net.ServiceProxy;
import com.doodream.rmovjs.serde.Converter;
import com.doodream.rmovjs.server.RMIController;
import com.google.gson.annotations.SerializedName;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.functions.Function;

import java.util.*;

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
    private Map<String, String> params;

    @SerializedName("interfaces")
    private List<ControllerInfo> controllerInfos;

    /**
     *  remoteHint is used to guess connection information (like address or bluetooth device name etc.,)
     *
     */
    @SerializedName("hint")
    private String proxyFactoryHint;



    public static class Builder {
        private final RMIServiceInfo serviceInfo = new RMIServiceInfo();
        private Builder() { }

        public Builder version(String version) {
            serviceInfo.version = version;
            return this;
        }

        public Builder adapter(Class<? extends ServiceAdapter> adapter) {
            serviceInfo.adapter = adapter;
            return this;
        }

        public Builder negotiator(Class<? extends Negotiator> negotiator) {
            serviceInfo.negotiator = negotiator;
            return this;
        }

        public Builder converter(Class<? extends Converter> converter) {
            serviceInfo.converter = converter;
            return this;
        }

        public Builder params(Map<String, String> params) {
            serviceInfo.params = params;
            return this;
        }

        public Builder provider(String provider) {
            serviceInfo.provider = provider;
            return this;
        }

        public Builder name(String name) {
            serviceInfo.name = name;
            return this;
        }

        public Builder controllerInfos(List<ControllerInfo> controllerInfos) {
            serviceInfo.controllerInfos = controllerInfos;
            return this;
        }

        public RMIServiceInfo build() {
            return serviceInfo;
        }
    }


    public static RMIServiceInfo from(Class<?> svc) {
        Service service = svc.getAnnotation(Service.class);
        final Builder builder = RMIServiceInfo.builder();

        Map<String, String> paramAsMap = Observable.fromArray(service.params())
                .collectInto(new HashMap<String, String>(), (map, param) -> map.put(param.key(), param.value()))
                .blockingGet();

        builder.version(Properties.getVersionString())
                .adapter(service.adapter())
                .negotiator(service.negotiator())
                .converter(service.converter())
                .params(paramAsMap)
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

    public static Builder builder() {
        return new Builder();
    }


    public static boolean isValid(RMIServiceInfo info) {
        return (info.getProxyFactoryHint() != null) &&
                (info.getControllerInfos() != null);
    }


    public void setProxyFactoryHint(String proxyFactoryHint) {
        this.proxyFactoryHint = proxyFactoryHint;
    }

    public void setControllerInfos(List<ControllerInfo> controllerInfos) {
        this.controllerInfos = controllerInfos;
    }

    public String getProxyFactoryHint() {
        return proxyFactoryHint;
    }

    public List<ControllerInfo> getControllerInfos() {
        return controllerInfos;
    }

    public Map<String, String> getParams() {
        return params;
    }

    public Class getAdapter() {
        return adapter;
    }

    public Class getConverter() {
        return converter;
    }

    public String getAlias() {
        return alias;
    }

    public Class getNegotiator() {
        return negotiator;
    }

    public String getName() {
        return name;
    }

    public String getProvider() {
        return provider;
    }

    public String getVersion() {
        return version;
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
        proxyFactoryHint = info.getProxyFactoryHint();
        params = info.getParams();
        adapter = info.getAdapter();
        controllerInfos = info.getControllerInfos();
        name = info.getName();
        alias = info.getAlias();
        negotiator = info.getNegotiator();
        provider = info.getProvider();
        version = info.getVersion();
    }

    @Override
    public String toString() {
        return String.format(Locale.ENGLISH,"{ params : %s, adapter : %s, controllerInfos : %s, name :%s, negotiator : %s, provider : %s, version : %s}", params, adapter, controllerInfos, name, negotiator, provider, version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(params, adapter, controllerInfos, name, negotiator, provider, version);
    }

    @Override
    public boolean equals(Object obj) {
        return hashCode() == obj.hashCode();
    }
}
