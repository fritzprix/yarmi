package net.doodream.yarmi.sdp;

import net.doodream.yarmi.model.RMIServiceInfo;
/**
 * @author fritzprix
 */
public interface ServiceDiscoveryListener {
    /**
     * 
     */
    int FINISH_NORMAL = 0;
    int FINISH_ERROR = 1;

    /**
     * 
     */
    void onDiscoveryStarted();

    /**
     * 
     * @param info
     */
    void onServiceDiscovered(RMIServiceInfo info);

    /**
     * 
     * @param code
     * @param err
     */
    void onDiscoveryFinished(int code, Throwable err);
}
