package com.doodream.rmovjs.test;

import com.doodream.rmovjs.model.RMIServiceInfo;
import com.doodream.rmovjs.sdp.ServiceDiscoveryListener;
import com.doodream.rmovjs.sdp.SimpleServiceDiscovery;
import com.doodream.rmovjs.sdp.SimpleServiceRegistry;
import com.doodream.rmovjs.server.RMIService;
import com.doodream.rmovjs.test.service.echoback.EchoBackService;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.net.Inet4Address;
import java.util.Collection;

public class ServiceDiscoveryTest {

    private static SimpleServiceRegistry TEST_REGISTRY;
    private static final RMIService TEST_SERVICE = RMIService.create(EchoBackService.class);


    @BeforeClass
    public static void init() throws IOException, InstantiationException, IllegalAccessException {
        TEST_REGISTRY = new SimpleServiceRegistry(Inet4Address.getByName("192.168.32.4"));
        TEST_SERVICE.listen();
        TEST_REGISTRY.start();
        TEST_REGISTRY.register(TEST_SERVICE);
    }

    @AfterClass
    public static void exit() throws IOException {
        TEST_SERVICE.stop();
        TEST_REGISTRY.stop();
    }

    @Test
    public void testDiscovery() throws IOException {
        SimpleServiceDiscovery discovery = new SimpleServiceDiscovery(Inet4Address.getByName("192.168.32.4"));
        try {
            discovery.start(EchoBackService.class, new ServiceDiscoveryListener() {
                @Override
                public void onServiceDiscovered(Collection<RMIServiceInfo> infos) {
                    System.out.println(infos);
                }

                @Override
                public void onError(Throwable err) {

                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
