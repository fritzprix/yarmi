package net.doodream.yarmi.server.svc;

import net.doodream.yarmi.annotation.RMIExpose;
import net.doodream.yarmi.data.Response;

public interface HealthCheckController {
    @RMIExpose
    Response<Long> check();
}
