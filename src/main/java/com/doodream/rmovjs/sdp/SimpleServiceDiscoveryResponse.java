package com.doodream.rmovjs.sdp;

import com.doodream.rmovjs.model.RMIServiceInfo;
import com.google.gson.annotations.SerializedName;

import java.util.Set;

public class SimpleServiceDiscoveryResponse {

    @SerializedName("query")
    private SimpleServiceQuery query;

    @SerializedName("services")
    private Set<RMIServiceInfo> services;

    public void setQuery(SimpleServiceQuery query) {
        this.query = query;
    }

    public void setServices(Set<RMIServiceInfo> services) {
        this.services = services;
    }

    public Set<RMIServiceInfo> getServices() {
        return services;
    }

    public SimpleServiceQuery getQuery() {
        return query;
    }
}
