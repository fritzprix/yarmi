package net.doodream.yarmi.server.svc;

import net.doodream.yarmi.annotation.method.Get;
import net.doodream.yarmi.model.Response;

public interface HealthCheckController {
    @Get("/health/ping")
    Response<Long> check();
}
