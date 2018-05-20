package com.doodream.rmovjs;

import com.doodream.rmovjs.client.RMIClient;
import com.doodream.rmovjs.example.TestService;
import com.doodream.rmovjs.example.User;
import com.doodream.rmovjs.example.UserIDPController;
import com.doodream.rmovjs.model.RMIServiceInfo;
import com.doodream.rmovjs.model.Response;
import com.doodream.rmovjs.sdp.local.LocalServiceAdvertiser;
import com.doodream.rmovjs.sdp.local.LocalServiceDiscovery;
import com.doodream.rmovjs.server.RMIService;

import java.io.IOException;

public class Main {

    public static void main(String[] args) throws InterruptedException, IOException {
        new Thread(() -> {
            try {
                RMIService service = RMIService.create(TestService.class, new LocalServiceAdvertiser());
                service.listen();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

        RMIServiceInfo serviceInfo = RMIServiceInfo.from(TestService.class);
        LocalServiceDiscovery discovery = new LocalServiceDiscovery();
        discovery.startDiscovery(serviceInfo, discovered -> {
            assert discovered.provide(UserIDPController.class);

            // create client side controller for service
            UserIDPController userCtr = RMIClient.create(discovered, TestService.class, UserIDPController.class);

            // request to service
            User user = new User();
            user.setName("David");

            Response<User> response = userCtr.createUser(user);
            assert response.isSuccessful();
            response = userCtr.getUser(response.getBody().getId());
            assert response.isSuccessful();
            assert user.equals(response.getBody());
        });

        while (true) {
            Thread.sleep(1000L);
        }
    }
}
