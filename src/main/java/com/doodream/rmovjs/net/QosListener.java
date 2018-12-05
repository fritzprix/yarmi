package com.doodream.rmovjs.net;

public interface QosListener {
    void onQosUpdated(long measuredRttInMill);

    void onError(Throwable throwable);
}
