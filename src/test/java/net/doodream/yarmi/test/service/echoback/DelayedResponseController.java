package net.doodream.yarmi.test.service.echoback;

import net.doodream.yarmi.annotation.RMICallback;
import net.doodream.yarmi.annotation.RMIExpose;
import net.doodream.yarmi.data.Response;

public interface DelayedResponseController {

    @RMIExpose
    Response getDelayedResponse(long delay);


    @RMIExpose
    Response setCallback(@RMICallback(cls=Callback.class) Callback callback);

}
