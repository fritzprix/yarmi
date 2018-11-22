package com.doodream.rmovjs.sdp;

import com.doodream.rmovjs.model.RMIServiceInfo;
import com.doodream.rmovjs.serde.Converter;

import java.io.IOException;
import java.net.InetAddress;

public interface ServiceAdvertiser {

    /**
     * start to send multicast packet to advertise service through all possible network interfaces
     * @param info service information to be advertised
     * @param block block if true, otherwise return immediately
     * @throws IOException
     */
    void startAdvertiser(RMIServiceInfo info, boolean block) throws IOException;


    /**
     * start to send multicast packet to advertise service through given interface
     * @param info service information to be advertised
     * @param block block if true, otherwise return immediately
     * @param inf network interface
     * @throws IOException
     */
    void startAdvertiser(RMIServiceInfo info, boolean block, InetAddress inf) throws IOException;

    /**
     * stop advertising
     * @throws IOException
     */
    void stopAdvertiser() throws IOException;
}
