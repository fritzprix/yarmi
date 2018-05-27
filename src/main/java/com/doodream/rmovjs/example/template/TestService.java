package com.doodream.rmovjs.example.template;

import com.doodream.rmovjs.annotation.server.Controller;
import com.doodream.rmovjs.annotation.server.Service;
import com.doodream.rmovjs.example.server.UserIDControllerImpl;
import com.doodream.rmovjs.net.SimpleNegotiator;
import com.doodream.rmovjs.net.tcp.TcpServiceAdapter;
import com.doodream.rmovjs.serde.GsonConverter;

@Service(name = "test-service",
        negotiator = SimpleNegotiator.class,
        adapter = TcpServiceAdapter.class,
        converter = GsonConverter.class,
        params = {"6644"})
public class TestService {

    @Controller(path = "/user", version = 1, module = UserIDControllerImpl.class)
    UserIDPController userIDPService;
}

