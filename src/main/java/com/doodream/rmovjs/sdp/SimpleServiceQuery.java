package com.doodream.rmovjs.sdp;

import com.doodream.rmovjs.model.RMIServiceInfo;
import com.google.gson.annotations.SerializedName;

import java.util.Locale;

public class SimpleServiceQuery {

    public static class Builder {
        
    }

    @SerializedName("nm")
    String name;

    @SerializedName("prv")
    String provider;

    @SerializedName("code")
    String hash;

    @SerializedName("cbp")
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
