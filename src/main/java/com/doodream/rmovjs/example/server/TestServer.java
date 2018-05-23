package com.doodream.rmovjs.example.server;

import com.doodream.rmovjs.example.template.TestService;
import com.doodream.rmovjs.sdp.SimpleServiceAdvertiser;
import com.doodream.rmovjs.server.RMIService;

public class TestServer {

    public static void main(String[] args) throws Exception {
        RMIService service = RMIService.create(TestService.class, new SimpleServiceAdvertiser());
        service.listen(true);
    }
}
