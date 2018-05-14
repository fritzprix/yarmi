package com.doodream.rmovjs.example;

import com.doodream.rmovjs.annotation.server.Controller;
import com.doodream.rmovjs.annotation.server.Service;

@Service(name = "test-service",
        params = {"127.0.0.1", "6644"})
public class TestService {

    @Controller(path = "/user", version = 1, module = UserIDControllerImpl.class)
    UserIDPController userIDPService;
}

