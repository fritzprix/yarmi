package com.doodream.rmovjs.sdp;

import com.doodream.rmovjs.model.ServiceInfo;

import java.io.IOException;

public interface RMIServiceAdvertiser {
    void startAdvertiser(ServiceInfo info) throws IOException;
    void stopAdvertiser() throws IOException;
}
