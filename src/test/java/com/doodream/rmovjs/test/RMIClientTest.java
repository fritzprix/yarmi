package com.doodream.rmovjs.test;

import com.doodream.rmovjs.client.RMIClient;
import com.doodream.rmovjs.model.ServiceInfo;
import com.doodream.rmovjs.sdp.local.LocalServiceAdvertiser;
import com.doodream.rmovjs.sdp.local.LocalServiceDiscovery;
import com.doodream.rmovjs.server.RMIService;
import com.doodream.rmovjs.test.service.TestService;
import com.doodream.rmovjs.test.service.UserIDPController;
import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RMIClientTest {

    private CompositeDisposable compositeDisposable;
    private RMIService server;

    @Before
    public void setup() throws Exception {
        server = RMIService.create(TestService.class, new LocalServiceAdvertiser());

        compositeDisposable = new CompositeDisposable();
        compositeDisposable.add(Observable.just(server)
                .doOnNext(RMIService::listen)
                .subscribeOn(Schedulers.newThread()).subscribe());
    }

    @After
    public void exit() throws Exception {
        while (compositeDisposable.isDisposed()) {
            Thread.sleep(1000L);
        }
        server.stop();
        compositeDisposable.clear();
    }

    @Test
    public void createTestClient() throws InstantiationException, IllegalAccessException, IOException {
        LocalServiceDiscovery serviceDiscovery = new LocalServiceDiscovery();
        ServiceInfo serviceInfo = ServiceInfo.from(TestService.class);
        compositeDisposable.add(serviceDiscovery.startDiscovery(serviceInfo)
                .map(serviceProxy -> RMIClient.<UserIDPController>create(serviceProxy, UserIDPController.class))
                .subscribe(controller -> {
                    controller.getUser(1L);
                    controller.getUsers();
                }));
    }
}
