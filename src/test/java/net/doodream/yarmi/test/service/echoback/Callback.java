package net.doodream.yarmi.test.service.echoback;

import net.doodream.yarmi.annotation.RMIExpose;
import net.doodream.yarmi.data.Response;


public interface Callback {

    @RMIExpose
    Response callback(long value);
}
