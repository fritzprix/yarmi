package com.doodream.rmovjs.server.svc.impl;

import com.doodream.rmovjs.model.Response;
import com.doodream.rmovjs.server.svc.HealthCheckController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HealthCheckControllerImpl implements HealthCheckController {
    private static final Logger Log = LoggerFactory.getLogger(HealthCheckControllerImpl.class);

    @Override
    public Response<Long> check() {
        Long timeStamp = System.currentTimeMillis();
        Log.debug("Health Check : {}", timeStamp);
        return Response.success(timeStamp);
    }
}
