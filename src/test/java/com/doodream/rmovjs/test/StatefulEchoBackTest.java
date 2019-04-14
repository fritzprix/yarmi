package com.doodream.rmovjs.test;

import com.doodream.rmovjs.client.RMIClient;
import com.doodream.rmovjs.model.RMIServiceInfo;
import com.doodream.rmovjs.model.Response;
import com.doodream.rmovjs.net.ServiceProxy;
import com.doodream.rmovjs.net.session.BlobSession;
import com.doodream.rmovjs.sdp.SilentServiceAdvertiser;
import com.doodream.rmovjs.server.RMIService;
import com.doodream.rmovjs.test.service.*;
import org.junit.*;
import org.junit.runners.MethodSorters;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
// some test cases are affected by test order
public class StatefulEchoBackTest {

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
    public void A_stringEchoBack() throws Exception {
        final String msg = "Hello";
        Object client = buildNewClient();
        EchoBackController messageController = (EchoBackController) client;


        Response response = messageController.sendMessage(msg);
        Assert.assertNotNull(response);
        Assert.assertTrue(response.isSuccessful());
        Assert.assertEquals(msg, response.getBody());
        Assert.assertFalse(response.hasScm());
        Assert.assertFalse(response.isHasSessionSwitch());
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
        Assert.assertFalse(response.isHasSessionSwitch());
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
        Assert.assertFalse(response.isHasSessionSwitch());
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
        Assert.assertFalse(response.isHasSessionSwitch());
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
        Assert.assertFalse(response.isHasSessionSwitch());
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
        Assert.assertFalse(response.isHasSessionSwitch());
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
        Assert.assertFalse(response.isHasSessionSwitch());
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
        Assert.assertFalse(response.isHasSessionSwitch());
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
        Assert.assertFalse(response.isHasSessionSwitch());
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
        Assert.assertFalse(response.isHasSessionSwitch());
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

            }
            finally {
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
        Assert.assertFalse(response.isHasSessionSwitch());
        Assert.assertEquals(Response.SUCCESS, response.getCode());

        RMIClient.destroy(client);
    }


    private Object buildNewClient() {
        final ServiceProxy proxy = RMIServiceInfo.toServiceProxy(serviceAdvertiser.getServiceInfo());
        Assert.assertNotNull(proxy);
        return RMIClient.create(proxy, TestService.class, new Class[]{
                UserIDPController.class,
                EchoBackController.class
        }, 10000L, TimeUnit.MILLISECONDS);
    }
}
