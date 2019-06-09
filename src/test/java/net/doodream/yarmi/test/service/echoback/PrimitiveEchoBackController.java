package net.doodream.yarmi.test.service.echoback;

import net.doodream.yarmi.annotation.method.RMIExpose;
import net.doodream.yarmi.annotation.parameter.Query;
import net.doodream.yarmi.model.Response;

public interface PrimitiveEchoBackController {

    @RMIExpose
    Response<Integer> echoBackInteger(@Query(name = "value") int v);
    @RMIExpose
    Response<Long> echoBackLong(@Query(name = "value") long v);
    @RMIExpose
    Response<Float> echoBackFloat(@Query(name = "value") float v);

    @RMIExpose
    Response<Double> echoBackDouble(@Query(name = "value") double v);

    @RMIExpose
    Response<Boolean> echoBackBoolean(@Query(name = "value") boolean v);

}
