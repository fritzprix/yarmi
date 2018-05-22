package com.doodream.rmovjs.sdp;

import com.doodream.rmovjs.net.RMIServiceProxy;

public interface ServiceDiscoveryListener {
    void onDiscovered(RMIServiceProxy proxy);
    void onDiscoveryStarted();
    void onDiscoveryFinished();
}
