package net.doodream.yarmi.sdp;

import net.doodream.yarmi.model.RMIServiceInfo;

import java.util.Locale;

public class SimpleServiceQuery {

    public static class Builder {
        
    }

    String name;

    String provider;

    String hash;

    int callbackPort;

    public static SimpleServiceQuery from(Class service, int port) {
        final RMIServiceInfo serviceInfo = RMIServiceInfo.from(service);

        SimpleServiceQuery query = new SimpleServiceQuery();
        query.callbackPort = port;
        query.hash = String.format(Locale.ENGLISH, "%x",serviceInfo.hashCode());
        query.name = serviceInfo.getName();
        query.provider = serviceInfo.getProvider();

        return query;
    }
}
