package com.doodream.rmovjs.sdp;

import com.doodream.rmovjs.model.RMIServiceInfo;
import com.doodream.rmovjs.serde.Converter;

import java.io.IOException;

public interface ServiceAdvertiser {
    /**
     * start service advertising
     * should not block
     * @param info
     * @param block
     * @throws IOException
     */
    void startAdvertiser(RMIServiceInfo info, boolean block) throws IOException;

    /**
     * stop advertising
     * @throws IOException
     */
    void stopAdvertiser() throws IOException;
}
