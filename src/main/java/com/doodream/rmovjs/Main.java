package com.doodream.rmovjs;

import com.doodream.rmovjs.client.RMIClient;
import com.doodream.rmovjs.example.TestService;
import com.doodream.rmovjs.example.User;
import com.doodream.rmovjs.example.UserIDPController;
import com.doodream.rmovjs.model.RMIServiceInfo;
import com.doodream.rmovjs.model.Response;
import com.doodream.rmovjs.net.RMIServiceProxy;
import com.doodream.rmovjs.sdp.ServiceDiscoveryListener;
import com.doodream.rmovjs.sdp.local.SimpleServiceAdvertiser;
import com.doodream.rmovjs.sdp.local.SimpleServiceDiscovery;
import com.doodream.rmovjs.server.RMIService;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Main {

    public static void main(String[] args) throws InterruptedException, IOException {
        new Thread(() -> {
            try {
                RMIService service = RMIService.create(TestService.class, new SimpleServiceAdvertiser());
                service.listen();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
        List<RMIServiceProxy> discoveredService = new LinkedList<>();

        RMIServiceInfo serviceInfo = RMIServiceInfo.from(TestService.class);
        SimpleServiceDiscovery discovery = new SimpleServiceDiscovery();
        discovery.startDiscovery(serviceInfo, new ServiceDiscoveryListener() {
            @Override
            public void onDiscovered(RMIServiceProxy proxy)  {
                System.out.println("Discovered :  " + proxy);
                discoveredService.add(proxy);
            }

            @Override
            public void onDiscoveryStarted() {
                System.out.println("Discovery Started");
                discovery.cancelDiscovery(serviceInfo);
            }

            @Override
            public void onDiscoveryFinished() {
                if(discoveredService.size() > 0) {
                    RMIServiceProxy serviceProxy = discoveredService.get(0);
                    assert serviceProxy.provide(UserIDPController.class);

                  // create client side controller for service
                    try {
                        UserIDPController userCtr = RMIClient.create(serviceProxy, TestService.class, UserIDPController.class);
                        User user = new User();
                        user.setName("David");

                        Response<User> response = userCtr.createUser(user);
                        assert response.isSuccessful();
                        response = userCtr.getUser(1L);
                        assert !response.isSuccessful();
                        assert user.equals(response.getBody());
                    } catch (IllegalAccessException | InstantiationException | IOException e) {
                        e.printStackTrace();
                    }


                }
            }
        }, 2L, TimeUnit.SECONDS);

        while (true) {
            Thread.sleep(1000L);
        }
    }
}
