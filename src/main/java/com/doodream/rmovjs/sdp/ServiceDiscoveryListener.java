package com.doodream.rmovjs.sdp;

import com.doodream.rmovjs.model.RMIServiceInfo;

public interface ServiceDiscoveryListener {
    void onDiscovered(RMIServiceInfo info);
    void onDiscoveryStarted();
    void onDiscoveryFinished() throws IllegalAccessException;
}
