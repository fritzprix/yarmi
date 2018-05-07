package com.doodream.rmovjs.test;

import com.doodream.rmovjs.server.RMIService;
import com.doodream.rmovjs.test.service.TestService;
import org.junit.Before;
import org.junit.Test;

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
        RMIService server = RMIService.create(TestService.class);
        executor.execute(() -> {
            try {
                server.listen();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }



}
