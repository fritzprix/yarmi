package net.doodream.yarmi.test.service.echoback;

import net.doodream.yarmi.annotation.AdapterParam;
import net.doodream.yarmi.annotation.server.Controller;
import net.doodream.yarmi.annotation.server.Service;
import net.doodream.yarmi.net.tcp.TcpServiceAdapter;
import net.doodream.yarmi.serde.bson.BsonConverter;

@Service(
        name = "test-service",
        provider = "www.doodream.com",
        converter = BsonConverter.class,
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

