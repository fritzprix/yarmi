package com.doodream.rmovjs.test.service.echoback;

import com.doodream.rmovjs.annotation.method.RMIExpose;
import com.doodream.rmovjs.annotation.parameter.Query;
import com.doodream.rmovjs.model.Response;

public interface DelayedResponseController {

    @RMIExpose
    Response getDelayedResponse(@Query(name = "delay") long delay);
}
