package net.doodream.yarmi.test.service.noreply;


import net.doodream.yarmi.annotation.server.Controller;
import net.doodream.yarmi.annotation.server.Service;
import net.doodream.yarmi.serde.bson.BsonConverter;
import net.doodream.yarmi.test.net.noreply.BypassNegotiator;
import net.doodream.yarmi.test.net.noreply.NoReplyServiceAdapter;
import net.doodream.yarmi.test.service.echoback.EchoBackController;
import net.doodream.yarmi.test.service.echoback.EchoBackControllerImpl;

@Service(name = "no-reply",
        provider = "www.doodream.com",
        adapter = NoReplyServiceAdapter.class,
        negotiator = BypassNegotiator.class,
        converter = BsonConverter.class)
public class NoReplyService {

    @Controller(path = "/echo-back", version = 1,  module = EchoBackControllerImpl.class)
    EchoBackController echoBackController;
}
