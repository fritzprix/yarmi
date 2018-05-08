package com.doodream.rmovjs.test;

import com.doodream.rmovjs.sdp.local.LocalServiceAdvertiser;
import com.doodream.rmovjs.server.RMIService;
import com.doodream.rmovjs.test.service.TestService;
import org.junit.Before;
import org.junit.Test;
import org.omg.PortableInterceptor.SYSTEM_EXCEPTION;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class RMIServerTest {


    private Executor executor;
    @Before
    public void setup() {
        executor = Executors.newFixedThreadPool(4);
    }

    @Test
    public void runServer() throws Exception {
        RMIService server = RMIService.create(TestService.class, new LocalServiceAdvertiser());
        server.listen();
    }



}
