package net.doodream.yarmi.test;

import net.doodream.yarmi.annotation.RMIException;
import net.doodream.yarmi.client.RMIClient;
import net.doodream.yarmi.data.RMIServiceInfo;
import net.doodream.yarmi.net.ServiceProxy;
import net.doodream.yarmi.server.RMIService;
import net.doodream.yarmi.test.service.echoback.EchoBackController;
import net.doodream.yarmi.test.service.noreply.NoReplyService;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class ServiceNoReplyTest {

    private static RMIService service;

    @BeforeClass
    public static void startServer() throws Exception {
        service = RMIService.create(NoReplyService.class);
        service.listen();
    }

    @AfterClass
    public static void stopServer() throws Exception {
        service.stop();
    }

    @Test(expected = RMIException.class)
    public void testNoReply() throws Exception {
        Object client = buildNewClient();
        final EchoBackController controller = (EchoBackController) client;
        controller.sendUserList(new ArrayList<>());
        RMIClient.destroy(client);
    }

    private Object buildNewClient() {
        try {
            final ServiceProxy proxy = RMIServiceInfo.toServiceProxy(service.getServiceInfo());
            Assert.assertNotNull(proxy);
            return RMIClient.create(proxy, NoReplyService.class, new Class[]{
                    EchoBackController.class,
            }, 1000L, TimeUnit.MILLISECONDS);
        } catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }
}
