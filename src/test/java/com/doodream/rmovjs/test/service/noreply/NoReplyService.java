package com.doodream.rmovjs.test.service.noreply;


import com.doodream.rmovjs.annotation.server.Controller;
import com.doodream.rmovjs.annotation.server.Service;
import com.doodream.rmovjs.test.net.noreply.BypassNegotiator;
import com.doodream.rmovjs.test.net.noreply.NoReplyServiceAdapter;
import com.doodream.rmovjs.test.service.echoback.EchoBackController;
import com.doodream.rmovjs.test.service.echoback.EchoBackControllerImpl;

@Service(name = "no-reply",
        provider = "www.doodream.com",
        adapter = NoReplyServiceAdapter.class,
        negotiator = BypassNegotiator.class)
public class NoReplyService {

    @Controller(path = "/echo-back", version = 1,  module = EchoBackControllerImpl.class)
    EchoBackController echoBackController;
}
