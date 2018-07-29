package com.doodream.rmovjs.server;

import com.doodream.rmovjs.annotation.server.Controller;
import com.doodream.rmovjs.server.svc.HealthCheckController;
import com.doodream.rmovjs.server.svc.impl.HealthCheckControllerImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;

public class BasicService {
    private static Logger Log = LoggerFactory.getLogger(BasicService.class);

    @Controller(version = 0,module = HealthCheckControllerImpl.class, path = "/")
    public HealthCheckController healthCheckController;

    public static Controller getHealthCheckController()  {
        try {
            Field field = BasicService.class.getDeclaredField("healthCheckController");
            return field.getAnnotation(Controller.class);
        } catch (NoSuchFieldException e) {
            Log.error(e.getLocalizedMessage());
            return null;
        }
    }
}
