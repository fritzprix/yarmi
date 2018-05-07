package com.doodream.rmovjs.test.service;

import com.doodream.rmovjs.annotation.server.Controller;
import com.doodream.rmovjs.annotation.server.Service;

@Service(name = "test-service")
public class TestService {

    @Controller(path = "/user",module = UserIDControllerImpl.class)
    UserIDPController userIDPService;
}

