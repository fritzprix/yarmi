package net.doodream.yarmi.sdp;

import net.doodream.yarmi.model.RMIServiceInfo;

import java.util.Collection;

public interface ServiceDiscoveryListener {
    void onServiceDiscovered(Collection<RMIServiceInfo> infos);
    void onError(Throwable err);
}
