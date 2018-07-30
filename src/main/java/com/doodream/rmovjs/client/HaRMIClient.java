package com.doodream.rmovjs.client;

import com.doodream.rmovjs.model.Endpoint;
import com.doodream.rmovjs.model.RMIServiceInfo;
import com.doodream.rmovjs.net.RMIServiceProxy;
import com.doodream.rmovjs.sdp.ServiceDiscovery;
import com.doodream.rmovjs.serde.Converter;
import com.doodream.rmovjs.util.LruCache;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Map;

/**
 *  High-Available RMI Client
 */
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class HaRMIClient implements InvocationHandler {

    /**
     * policy
     */
    public enum RequestRoutePolicy {
        RoundRobin,

    }

    private static final Logger Log = LoggerFactory.getLogger(HaRMIClient.class);

    private String controllerPath;
    private Map<Method, Endpoint> methodMap;
    private RMIServiceProxy serviceProxy;
    private RMIServiceInfo serviceInfo;
    private Converter converter;

    private ServiceDiscovery discovery;
    private LruCache<String, RMIServiceProxy> proxies;

    public static <T> T create(ServiceDiscovery discovery, Class svc, Class<T> ctrl) throws IllegalAccessException, IOException, InstantiationException {
        return null;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        return null;
    }
}
