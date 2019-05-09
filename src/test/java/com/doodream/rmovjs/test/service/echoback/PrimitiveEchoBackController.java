package com.doodream.rmovjs.test.service.echoback;

import com.doodream.rmovjs.annotation.method.RMIExpose;
import com.doodream.rmovjs.annotation.parameter.Query;
import com.doodream.rmovjs.model.Response;

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
