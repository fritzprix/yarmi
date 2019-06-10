package net.doodream.yarmi.sdp;

import net.doodream.yarmi.model.RMIServiceInfo;

import java.util.Set;

public class SimpleServiceDiscoveryResponse {

    private SimpleServiceQuery query;

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
