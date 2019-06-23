package net.doodream.yarmi.test.service.echoback;

import net.doodream.yarmi.data.Response;

public class DelayedResponseControllerImpl implements DelayedResponseController{
    @Override
    public Response getDelayedResponse(long delay) {
        try {
            Thread.sleep(delay);
        } catch (InterruptedException ignored) { }
        return Response.success(delay);
    }
}
