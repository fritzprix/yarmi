package com.doodream.rmovjs.service;

import com.doodream.rmovjs.annotation.method.RMIExpose;
import com.doodream.rmovjs.annotation.parameter.Body;
import com.doodream.rmovjs.annotation.parameter.Query;
import com.doodream.rmovjs.model.RMIServiceInfo;
import com.doodream.rmovjs.model.Response;

public interface ServiceRegistryController {

    @RMIExpose
    Response<Integer> register(@Body RMIServiceInfo service);

    @RMIExpose
    Response unregister(@Query(name = "id") int id);
}
