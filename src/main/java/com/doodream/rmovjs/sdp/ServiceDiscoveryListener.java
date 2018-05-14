package com.doodream.rmovjs.sdp;

import com.doodream.rmovjs.net.RMIServiceProxy;

import java.io.IOException;

public interface ServiceDiscoveryListener {
    void onDiscovered(RMIServiceProxy proxy) throws IOException, InstantiationException, IllegalAccessException;
}
