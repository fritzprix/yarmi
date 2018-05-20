package com.doodream.rmovjs.example;

import com.doodream.rmovjs.annotation.server.Controller;
import com.doodream.rmovjs.annotation.server.Service;
import com.doodream.rmovjs.net.inet.InetServiceAdapter;

@Service(name = "test-service",
        adapter = InetServiceAdapter.class,
        params = {"127.0.0.1", "6644"})
public class TestService {

    @Controller(path = "/user", version = 1, module = UserIDControllerImpl.class)
    UserIDPController userIDPService;
}

