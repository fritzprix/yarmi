package com.doodream.rmovjs.test.service.echoback;

import com.doodream.rmovjs.annotation.parameter.AdapterParam;
import com.doodream.rmovjs.annotation.server.Controller;
import com.doodream.rmovjs.annotation.server.Service;
import com.doodream.rmovjs.net.tcp.TcpServiceAdapter;

@Service(
        name = "test-service",
        provider = "www.doodream.com",
        params = {
            @AdapterParam(key= TcpServiceAdapter.PARAM_PORT, value = "6464")
        })
public class EchoBackService {

    @Controller(path = "/echo/object", version = 1, module = EchoBackControllerImpl.class)
    EchoBackController echoBackController;

    @Controller(path = "/echo/primitive", version = 1, module = PrimitiveEchoBackControllerImpl.class)
    PrimitiveEchoBackController primitiveEchoBackController;

    @Controller(path = "/delayed/response", version = 1, module = DelayedResponseControllerImpl.class)
    DelayedResponseController delayedResponseController;

}

