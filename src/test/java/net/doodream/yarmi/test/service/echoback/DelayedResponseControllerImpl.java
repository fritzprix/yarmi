package net.doodream.yarmi.test.service.echoback;

import net.doodream.yarmi.data.Response;

public class DelayedResponseControllerImpl implements DelayedResponseController {
    private Callback callback;

    @Override
    public Response getDelayedResponse(long delay) {
        try {
            Thread.sleep(delay);
        } catch (InterruptedException ignored) {
        }
        return Response.success(delay);
    }

    @Override
    public Response setCallback(Callback callback) {
        this.callback = callback;
        return Response.success(0);
    }
}
