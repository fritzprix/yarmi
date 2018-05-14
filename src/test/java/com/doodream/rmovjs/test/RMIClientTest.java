package com.doodream.rmovjs.test;

import com.doodream.rmovjs.model.RMIServiceInfo;
import com.doodream.rmovjs.sdp.local.LocalServiceAdvertiser;
import com.doodream.rmovjs.sdp.local.LocalServiceDiscovery;
import com.doodream.rmovjs.server.RMIService;
import com.doodream.rmovjs.test.service.TestService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

public class RMIClientTest {

    private RMIService service;

    @Before
    public void setup() throws Exception {
        LocalServiceAdvertiser advertiser = new LocalServiceAdvertiser();
        service = RMIService.create(TestService.class, advertiser);
        new Thread(()-> {
            try {
                service.listen();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    @After
    public void exit() throws Exception {
    }

    @Test
    public void createTestClient() throws InstantiationException, IllegalAccessException, IOException, InterruptedException {
        LocalServiceDiscovery serviceDiscovery = new LocalServiceDiscovery();
        RMIServiceInfo serviceInfo = RMIServiceInfo.from(TestService.class);
        serviceDiscovery.startDiscovery(serviceInfo, System.out::println);
    }
}
