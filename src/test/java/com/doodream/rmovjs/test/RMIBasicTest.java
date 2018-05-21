package com.doodream.rmovjs.test;

import com.doodream.rmovjs.sdp.local.SimpleServiceAdvertiser;
import com.doodream.rmovjs.server.RMIService;
import com.doodream.rmovjs.test.service.TestService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

public class RMIBasicTest {

    private RMIService service;
    private Thread server;

    @Before
    public void setup() throws Exception {
        SimpleServiceAdvertiser advertiser = new SimpleServiceAdvertiser();
        service = RMIService.create(TestService.class, advertiser);
        server = new Thread(() -> {
            try {
                service.listen();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        server.start();
    }

    @After
    public void exit() throws Exception {
        service.stop();
    }

    @Test
    public void createTestClient() throws IOException, InterruptedException {
        AtomicReference<Boolean> isDiscovered = new AtomicReference<>();
    }
}
