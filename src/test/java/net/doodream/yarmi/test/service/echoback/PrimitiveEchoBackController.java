package net.doodream.yarmi.test.service.echoback;

import net.doodream.yarmi.annotation.RMIExpose;
import net.doodream.yarmi.data.Response;

public interface PrimitiveEchoBackController {

    @RMIExpose
    Response<Integer> echoBackInteger(int v);
    @RMIExpose
    Response<Long> echoBackLong(long v);
    @RMIExpose
    Response<Float> echoBackFloat(float v);

    @RMIExpose
    Response<Double> echoBackDouble(double v);

    @RMIExpose
    Response<Boolean> echoBackBoolean(boolean v);

}
