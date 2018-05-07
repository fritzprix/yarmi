package com.doodream.rmovjs.test;

import com.doodream.rmovjs.client.RMIClient;
import com.doodream.rmovjs.test.service.UserIDPController;
import org.junit.Test;

public class RMIClientTest {

    @Test
    public void createTestClient() throws InstantiationException, IllegalAccessException {
        UserIDPController controller = RMIClient.<UserIDPController>create(UserIDPController.class);
        controller.getUsers();
        controller.getUser(1L);
    }
}
