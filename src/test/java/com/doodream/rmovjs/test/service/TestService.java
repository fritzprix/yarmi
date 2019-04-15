package com.doodream.rmovjs.test.service;

import com.doodream.rmovjs.annotation.parameter.AdapterParam;
import com.doodream.rmovjs.annotation.server.Controller;
import com.doodream.rmovjs.annotation.server.Service;
import com.doodream.rmovjs.net.tcp.TcpServiceAdapter;

@Service(
        name = "test-service",
        provider = "www.doodream.com",
        params = {
            @AdapterParam(key= TcpServiceAdapter.PARAM_PORT, value = "6644")
        })
public class TestService {

    @Controller(path = "/user", version = 1, module = UserIDControllerImpl.class)
    UserIDPController userIDPController;

    @Controller(path = "/message", version = 1, module = EchoBackControllerImpl.class)
    EchoBackController messageController;
}

