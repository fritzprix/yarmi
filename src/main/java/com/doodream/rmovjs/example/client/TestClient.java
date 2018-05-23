package com.doodream.rmovjs.example.client;

import com.doodream.rmovjs.client.RMIClient;
import com.doodream.rmovjs.example.template.TestService;
import com.doodream.rmovjs.example.template.User;
import com.doodream.rmovjs.example.template.UserIDPController;
import com.doodream.rmovjs.model.Response;
import com.doodream.rmovjs.net.RMIServiceProxy;
import com.doodream.rmovjs.sdp.ServiceDiscoveryListener;
import com.doodream.rmovjs.sdp.SimpleServiceDiscovery;
import com.google.common.base.Preconditions;

import java.io.IOException;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

public class TestClient {

    public static void main(String[] args) throws SocketException, InterruptedException {
        List<RMIServiceProxy> proxies = new ArrayList<>();
        new SimpleServiceDiscovery().startDiscovery(TestService.class, new ServiceDiscoveryListener() {
            @Override
            public void onDiscovered(RMIServiceProxy proxy) {
                proxies.add(proxy);
            }

            @Override
            public void onDiscoveryStarted() {

            }

            @Override
            public void onDiscoveryFinished() throws IllegalAccessException, IOException, InstantiationException {
                if(proxies.size() > 0) {
                    RMIServiceProxy proxy = proxies.get(0);
                    Preconditions.checkNotNull(proxy);

                    UserIDPController controller = RMIClient.create(proxy, TestService.class, UserIDPController.class);
                    Preconditions.checkNotNull(controller);

                    User user = new User();
                    user.setName("nu100");

                    Response<User> response = controller.createUser(user);

                    Preconditions.checkNotNull(response);
                    Preconditions.checkArgument(response.isSuccessful());
                    User created = response.getBody();
                    Preconditions.checkNotNull(created);
                }
            }
        });

        while(true) {
            Thread.sleep(1000L);
        }
    }
}
