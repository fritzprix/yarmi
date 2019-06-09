package net.doodream.yarmi.test.service.echoback;

import net.doodream.yarmi.model.Response;

public class PrimitiveEchoBackControllerImpl implements PrimitiveEchoBackController {
    @Override
    public Response<Integer> echoBackInteger(int v) {
        return Response.success(v);
    }

    @Override
    public Response<Long> echoBackLong(long v) {
        return Response.success(v);
    }

    @Override
    public Response<Float> echoBackFloat(float v) {
        return Response.success(v);
    }

    @Override
    public Response<Double> echoBackDouble(double v) {
        return Response.success(v);
    }

    @Override
    public Response<Boolean> echoBackBoolean(boolean v) {
        return Response.success(v);
    }
}
