package com.doodream.rmovjs.sdp;

import com.doodream.rmovjs.model.RMIServiceInfo;

import java.io.IOException;

public interface ServiceAdvertiser {
    /**
     * start service advertising
     * should not block
     * @param info
     * @throws IOException
     */
    void startAdvertiser(RMIServiceInfo info) throws IOException;

    /**
     * stop advertising
     * @throws IOException
     */
    void stopAdvertiser() throws IOException;
}
