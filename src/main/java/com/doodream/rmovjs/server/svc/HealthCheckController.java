package com.doodream.rmovjs.server.svc;

import com.doodream.rmovjs.Properties;
import com.doodream.rmovjs.annotation.method.Get;
import com.doodream.rmovjs.model.Response;

public interface HealthCheckController {
    @Get("/health/ping")
    Response<Long> check();
}
