package com.doodream.rmovjs.sdp;

import com.doodream.rmovjs.model.RMIServiceInfo;

import java.util.Collection;

public interface ServiceDiscoveryListener {
    void onServiceDiscovered(Collection<RMIServiceInfo> infos);
    void onError(Throwable err);
}
