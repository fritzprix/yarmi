package com.doodream.rmovjs.test;

import com.doodream.rmovjs.net.RMIServiceProxy;
import com.doodream.rmovjs.sdp.ServiceDiscoveryListener;
import com.doodream.rmovjs.sdp.SimpleServiceAdvertiser;
import com.doodream.rmovjs.sdp.SimpleServiceDiscovery;
import com.doodream.rmovjs.server.RMIService;
import com.doodream.rmovjs.test.service.TestService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.SocketException;

public class RMIBasicTest {

    private RMIService service;

    @Before
    public void setup() throws Exception {
        SimpleServiceAdvertiser advertiser = new SimpleServiceAdvertiser();
        service = RMIService.create(TestService.class, advertiser);
        service.listen(false);
    }

    @After
    public void exit() throws Exception {
        service.stop();
    }

    @Test
    public void createTestClient() throws SocketException, IllegalAccessException, InstantiationException {
        new SimpleServiceDiscovery().startDiscovery(TestService.class, new ServiceDiscoveryListener() {
            @Override
            public void onDiscovered(RMIServiceProxy proxy) {

            }

            @Override
            public void onDiscoveryStarted() {

            }

            @Override
            public void onDiscoveryFinished() {

            }
        });
    }
}
