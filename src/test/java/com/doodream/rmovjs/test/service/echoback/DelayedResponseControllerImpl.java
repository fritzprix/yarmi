package com.doodream.rmovjs.test.service.echoback;

import com.doodream.rmovjs.model.Response;

public class DelayedResponseControllerImpl implements DelayedResponseController{
    @Override
    public Response getDelayedResponse(long delay) {
        try {
            Thread.sleep(delay);
        } catch (InterruptedException ignored) { }
        return Response.success(delay);
    }
}
