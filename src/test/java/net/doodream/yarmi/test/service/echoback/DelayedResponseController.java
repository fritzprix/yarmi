package net.doodream.yarmi.test.service.echoback;

import net.doodream.yarmi.annotation.method.RMIExpose;
import net.doodream.yarmi.annotation.parameter.Query;
import net.doodream.yarmi.model.Response;

public interface DelayedResponseController {

    @RMIExpose
    Response getDelayedResponse(@Query(name = "delay") long delay);
}
