package com.doodream.rmovjs.net;

public interface QosListener {
    void onQosUpdated(final ServiceProxy proxy, long measuredRttInMill);

    void onError(final ServiceProxy proxy, Throwable throwable);
}
