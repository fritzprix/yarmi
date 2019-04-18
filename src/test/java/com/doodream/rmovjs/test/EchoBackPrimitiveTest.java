package com.doodream.rmovjs.test;

import com.doodream.rmovjs.client.RMIClient;
import com.doodream.rmovjs.model.RMIServiceInfo;
import com.doodream.rmovjs.model.Response;
import com.doodream.rmovjs.net.ServiceProxy;
import com.doodream.rmovjs.sdp.SilentServiceAdvertiser;
import com.doodream.rmovjs.server.RMIService;
import com.doodream.rmovjs.test.service.EchoBackController;
import com.doodream.rmovjs.test.service.PrimitiveEchoBackController;
import com.doodream.rmovjs.test.service.TestService;
import com.doodream.rmovjs.test.service.UserIDPController;
import com.sun.org.apache.xpath.internal.operations.Bool;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

public class EchoBackPrimitiveTest {
    private static final SilentServiceAdvertiser serviceAdvertiser = new SilentServiceAdvertiser();
    private static RMIService service;

    @BeforeClass
    public static void startServer() throws Exception{
        service = RMIService.create(TestService.class, serviceAdvertiser);
        service.listen(false);

    }

    @AfterClass
    public static void stopServer() throws Exception {
        service.stop();
    }

    @Test
    public void testMethodCallIntegerParameter() {
        Object client = buildNewClient();
        PrimitiveEchoBackController controller = (PrimitiveEchoBackController) client;
        Response<Integer> response = controller.echoBackInteger(1);

        Assert.assertNotNull(response);
        Assert.assertTrue(response.isSuccessful());
        Assert.assertEquals(1, response.getBody().intValue());

        Assert.assertFalse(response.hasScm());
        Assert.assertFalse(response.isHasSessionSwitch());
    }


    @Test
    public void testMethodCallFloatParameter() {
        Object client = buildNewClient();
        PrimitiveEchoBackController controller = (PrimitiveEchoBackController) client;
        Response<Float> response = controller.echoBackFloat(1.f);

        Assert.assertNotNull(response);
        Assert.assertTrue(response.isSuccessful());
        Assert.assertEquals(0, response.getBody().compareTo(1.f));

        Assert.assertFalse(response.hasScm());
        Assert.assertFalse(response.isHasSessionSwitch());
    }

    @Test
    public void testMethodCallDoubleParameter() {
        Object client = buildNewClient();
        PrimitiveEchoBackController controller = (PrimitiveEchoBackController) client;
        Response<Double> response = controller.echoBackDouble(1.0);

        Assert.assertNotNull(response);
        Assert.assertTrue(response.isSuccessful());
        Assert.assertEquals(0, response.getBody().compareTo(1.0));

        Assert.assertFalse(response.hasScm());
        Assert.assertFalse(response.isHasSessionSwitch());
    }

    @Test
    public void testMethodCallLongParameter() {
        Object client = buildNewClient();
        PrimitiveEchoBackController controller = (PrimitiveEchoBackController) client;
        Response<Long> response = controller.echoBackLong(1L);

        Assert.assertNotNull(response);
        Assert.assertTrue(response.isSuccessful());
        Assert.assertEquals(0, response.getBody().compareTo(1L));

        Assert.assertFalse(response.hasScm());
        Assert.assertFalse(response.isHasSessionSwitch());
    }

    @Test
    public void testMethodCallBooleanParameter() {
        Object client = buildNewClient();
        PrimitiveEchoBackController controller = (PrimitiveEchoBackController) client;
        Response<Boolean> response = controller.echoBackBoolean(true);

        Assert.assertNotNull(response);
        Assert.assertTrue(response.isSuccessful());
        Assert.assertEquals(true, response.getBody());

        Assert.assertFalse(response.hasScm());
        Assert.assertFalse(response.isHasSessionSwitch());
    }

    private Object buildNewClient() {
        final ServiceProxy proxy = RMIServiceInfo.toServiceProxy(serviceAdvertiser.getServiceInfo());
        Assert.assertNotNull(proxy);
        return RMIClient.create(proxy, TestService.class, new Class[]{
                UserIDPController.class,
                EchoBackController.class,
                PrimitiveEchoBackController.class
        }, 10000L, TimeUnit.MILLISECONDS);
    }
}
