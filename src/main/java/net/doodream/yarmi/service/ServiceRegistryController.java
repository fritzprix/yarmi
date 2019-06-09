package net.doodream.yarmi.service;

import net.doodream.yarmi.annotation.method.RMIExpose;
import net.doodream.yarmi.annotation.parameter.Body;
import net.doodream.yarmi.annotation.parameter.Query;
import net.doodream.yarmi.model.RMIServiceInfo;
import net.doodream.yarmi.model.Response;

public interface ServiceRegistryController {

    @RMIExpose
    Response<Integer> register(@Body RMIServiceInfo service);

    @RMIExpose
    Response unregister(@Query(name = "id") int id);
}
