package net.doodream.yarmi.test;

import net.doodream.yarmi.annotation.RMIException;
import net.doodream.yarmi.client.RMIClient;
import net.doodream.yarmi.data.RMIServiceInfo;
import net.doodream.yarmi.data.Response;
import net.doodream.yarmi.net.ServiceProxy;
import net.doodream.yarmi.net.session.BlobSession;
import net.doodream.yarmi.server.RMIService;
import net.doodream.yarmi.test.data.ComplexObject;
import net.doodream.yarmi.test.data.User;
import net.doodream.yarmi.test.service.echoback.DelayedResponseController;
import net.doodream.yarmi.test.service.echoback.EchoBackController;
import net.doodream.yarmi.test.service.echoback.EchoBackService;
import net.doodream.yarmi.test.service.echoback.PrimitiveEchoBackController;
import org.junit.*;
import org.junit.runners.MethodSorters;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.*;
import java.util.concurrent.TimeUnit;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
// some test cases are affected by test order
public class EchoBackTest {

    private static RMIService service;
    private static ServiceProxy proxy;
    private static ServerSocket socket;

    @BeforeClass
    public static void startServer() throws Exception {
        try {
            service = RMIService.create(EchoBackService.class);
            service.listen();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @AfterClass
    public static void stopServer() throws Exception {
        service.stop();
    }

    @Test
    public void A_stringEchoBack() throws Exception {
        final String msg = "Hello";
        Object client = buildNewClient();
        EchoBackController messageController = (EchoBackController) client;

        Response response = messageController.sendMessage(msg);
        Assert.assertNotNull(response);
        Assert.assertTrue(response.isSuccessful());
        Assert.assertEquals(msg, response.getBody());
        Assert.assertFalse(response.hasScm());
        Assert.assertFalse(response.hasSessionSwitch());
        Assert.assertEquals(Response.SUCCESS, response.getCode());
        RMIClient.destroy(client);
    }

    @Test
    public void B_pojoEchoBack() throws Exception {
        final User user = User.builder()
                .name("David")
                .age(39)
                .build();

        final Object client = buildNewClient();
        final EchoBackController controller = (EchoBackController) client;

        final Response<User> response = controller.sendJavaObject(user);
        Assert.assertNotNull(response);
        Assert.assertTrue(response.isSuccessful());
        Assert.assertEquals(user, response.getBody());
        Assert.assertFalse(response.hasScm());
        Assert.assertFalse(response.hasSessionSwitch());
        Assert.assertEquals(Response.SUCCESS, response.getCode());

        RMIClient.destroy(client);
    }

    @Test
    public void C_listEchoBack() throws Exception {
        final List<String> list = Arrays.asList("HELLO", "THERE");

        final Object client = buildNewClient();
        final EchoBackController controller = (EchoBackController) client;

        final Response<List<String>> response = controller.sendList(list);
        Assert.assertNotNull(response);
        Assert.assertTrue(response.isSuccessful());

        Assert.assertEquals(list, response.getBody());
        Assert.assertFalse(response.hasScm());
        Assert.assertFalse(response.hasSessionSwitch());
        Assert.assertEquals(Response.SUCCESS, response.getCode());
        RMIClient.destroy(client);
    }

    @Test
    public void D_mapEchoBack() throws Exception {
        final Map<String, String> map = new HashMap<>();
        map.put("name", "david");

        final Object client = buildNewClient();
        final EchoBackController controller = (EchoBackController) client;

        final Response<Map<String, String>> response = controller.sendMap(map);
        Assert.assertNotNull(response);
        Assert.assertTrue(response.isSuccessful());

        Assert.assertEquals(map, response.getBody());
        Assert.assertFalse(response.hasScm());
        Assert.assertFalse(response.hasSessionSwitch());
        Assert.assertEquals(Response.SUCCESS, response.getCode());
        RMIClient.destroy(client);
    }

    @Test
    public void E_setEchoBack() throws Exception {
        final Set<String> set = new TreeSet<>();
        set.add("david");
        set.add("fritzprix");

        final Object client = buildNewClient();
        final EchoBackController controller = (EchoBackController) client;

        final Response<Set<String>> response = controller.sendSet(set);
        Assert.assertNotNull(response);
        Assert.assertTrue(response.isSuccessful());

        Assert.assertEquals(set, response.getBody());
        Assert.assertFalse(response.hasScm());
        Assert.assertFalse(response.hasSessionSwitch());
        Assert.assertEquals(Response.SUCCESS, response.getCode());
        RMIClient.destroy(client);
    }


    @Test
    public void F_userSetEchoBack() throws Exception {
        final Set<User> users = new HashSet<>();
        users.add(User.builder()
                .name("david")
                .age(39)
                .build());

        final Object client = buildNewClient();
        final EchoBackController controller = (EchoBackController) client;
        final Response<Set<User>> response = controller.sendUserSet(users);
        Assert.assertNotNull(response);
        Assert.assertTrue(response.isSuccessful());

        Assert.assertEquals(users, response.getBody());
        Assert.assertFalse(response.hasScm());
        Assert.assertFalse(response.hasSessionSwitch());
        Assert.assertEquals(Response.SUCCESS, response.getCode());


        RMIClient.destroy(client);
    }

    @Test
    public void G_userListEchoBack() throws Exception {
        final List<User> users = Arrays.asList(User.builder()
                .name("david")
                .age(39)
                .build());

        final Object client = buildNewClient();
        final EchoBackController controller = (EchoBackController) client;
        final Response<List<User>> response = controller.sendUserList(users);
        Assert.assertNotNull(response);
        Assert.assertTrue(response.isSuccessful());
        Assert.assertEquals(users, response.getBody());
        Assert.assertFalse(response.hasScm());
        Assert.assertFalse(response.hasSessionSwitch());
        Assert.assertEquals(Response.SUCCESS, response.getCode());

        RMIClient.destroy(client);
    }

    @Test
    public void H_userMapEchoBack() throws Exception {
        final Map<String, User> users = new HashMap<>();
        users.put("david", User.builder()
                .name("david")
                .age(39)
                .build());

        final Object client = buildNewClient();
        final EchoBackController controller = (EchoBackController) client;
        final Response<Map<String, User>> response = controller.sendUserMap(users);
        Assert.assertNotNull(response);
        Assert.assertTrue(response.isSuccessful());

        Assert.assertEquals(users, response.getBody());
        Assert.assertFalse(response.hasScm());
        Assert.assertFalse(response.hasSessionSwitch());
        Assert.assertEquals(Response.SUCCESS, response.getCode());

        RMIClient.destroy(client);
    }


    @Test
    public void I_userComplexObjectEchoBack() throws Exception {
        final ComplexObject object = ComplexObject.createTestObject();

        final Object client = buildNewClient();
        final EchoBackController controller = (EchoBackController) client;
        final Response<ComplexObject> response = controller.sendComplexObject(object);
        Assert.assertNotNull(response);
        Assert.assertTrue(response.isSuccessful());

        Assert.assertEquals(object, response.getBody());
        Assert.assertFalse(response.hasScm());
        Assert.assertFalse(response.hasSessionSwitch());
        Assert.assertEquals(Response.SUCCESS, response.getCode());

        RMIClient.destroy(client);
    }

    @Test
    public void J_userComplexObjectListEchoBack() throws Exception {
        final List<ComplexObject> objects = Arrays.asList(ComplexObject.createTestObject());

        final Object client = buildNewClient();
        final EchoBackController controller = (EchoBackController) client;
        final Response<List<ComplexObject>> response = controller.sendComplexObjectList(objects);
        Assert.assertNotNull(response);
        Assert.assertTrue(response.isSuccessful());

        Assert.assertEquals(objects, response.getBody());
        Assert.assertFalse(response.hasScm());
        Assert.assertFalse(response.hasSessionSwitch());
        Assert.assertEquals(Response.SUCCESS, response.getCode());

        RMIClient.destroy(client);
    }

    @Test
    public void K_uploadBlobTest() throws Exception {
        byte[] zeroFill = new byte[1 << 18];
        Arrays.fill(zeroFill, (byte) 0xA);
        BlobSession session = new BlobSession((ses) -> {
            try {
                ses.write(zeroFill, zeroFill.length);
            } catch (IOException ignored) {

            } finally {
                try {
                    ses.close();
                } catch (IOException ignored) {

                }
            }
        });

        final Object client = buildNewClient();
        final EchoBackController controller = (EchoBackController) client;
        final Response<Long> response = controller.sendBlob(session);
        Assert.assertNotNull(response);
        Assert.assertTrue(response.isSuccessful());

        Assert.assertEquals(Long.valueOf(0L), response.getBody());
        Assert.assertFalse(response.hasScm());
        Assert.assertFalse(response.hasSessionSwitch());
        Assert.assertEquals(Response.SUCCESS, response.getCode());

        RMIClient.destroy(client);
    }


    @Test
    public void testMethodCallIntegerParameter() throws Exception {
        Object client = buildNewClient();
        PrimitiveEchoBackController controller = (PrimitiveEchoBackController) client;
        Response<Integer> response = controller.echoBackInteger(1);

        Assert.assertNotNull(response);
        Assert.assertTrue(response.isSuccessful());
        Assert.assertEquals(1, response.getBody().intValue());

        Assert.assertFalse(response.hasScm());
        Assert.assertFalse(response.hasSessionSwitch());
        RMIClient.destroy(client);
    }


    @Test
    public void testMethodCallFloatParameter() throws Exception {
        Object client = buildNewClient();
        PrimitiveEchoBackController controller = (PrimitiveEchoBackController) client;
        Response<Float> response = controller.echoBackFloat(1.f);

        Assert.assertNotNull(response);
        Assert.assertTrue(response.isSuccessful());
        Assert.assertEquals(0, response.getBody().compareTo(1.f));

        Assert.assertFalse(response.hasScm());
        Assert.assertFalse(response.hasSessionSwitch());
        RMIClient.destroy(client);
    }

    @Test
    public void testMethodCallDoubleParameter() throws Exception {
        Object client = buildNewClient();
        PrimitiveEchoBackController controller = (PrimitiveEchoBackController) client;
        Response<Double> response = controller.echoBackDouble(1.0);

        Assert.assertNotNull(response);
        Assert.assertTrue(response.isSuccessful());
        Assert.assertEquals(0, response.getBody().compareTo(1.0));

        Assert.assertFalse(response.hasScm());
        Assert.assertFalse(response.hasSessionSwitch());
        RMIClient.destroy(client);
    }

    @Test
    public void testMethodCallLongParameter() throws Exception {
        Object client = buildNewClient();
        PrimitiveEchoBackController controller = (PrimitiveEchoBackController) client;
        Response<Long> response = controller.echoBackLong(1L);

        Assert.assertNotNull(response);
        Assert.assertTrue(response.isSuccessful());
        Assert.assertEquals(0, response.getBody().compareTo(1L));

        Assert.assertFalse(response.hasScm());
        Assert.assertFalse(response.hasSessionSwitch());
        RMIClient.destroy(client);
    }

    @Test
    public void testMethodCallBooleanParameter() throws Exception {
        Object client = buildNewClient();
        PrimitiveEchoBackController controller = (PrimitiveEchoBackController) client;
        Response<Boolean> response = controller.echoBackBoolean(true);

        Assert.assertNotNull(response);
        Assert.assertTrue(response.isSuccessful());
        Assert.assertEquals(true, response.getBody());

        Assert.assertFalse(response.hasScm());
        Assert.assertFalse(response.hasSessionSwitch());
        RMIClient.destroy(client);
    }


    private Object buildNewClient() {

        return RMIClient.create(service.getServiceInfo(), EchoBackService.class, new Class[]{
                DelayedResponseController.class,
                EchoBackController.class,
                PrimitiveEchoBackController.class
        }, 10000L, TimeUnit.MILLISECONDS);
    }
}
