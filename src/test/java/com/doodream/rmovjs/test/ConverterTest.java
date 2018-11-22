package com.doodream.rmovjs.test;


import com.doodream.rmovjs.model.Response;
import com.doodream.rmovjs.serde.Converter;
import com.doodream.rmovjs.serde.Reader;
import com.doodream.rmovjs.serde.Writer;
import com.doodream.rmovjs.serde.bson.BsonConverter;
import com.doodream.rmovjs.serde.json.JsonConverter;
import com.doodream.rmovjs.test.service.User;
import com.doodream.rmovjs.util.Types;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.*;

public class ConverterTest {


    private List<Converter> converters;

    @Before
    public void setup() {
        converters = Arrays.asList(
                new JsonConverter(),
                new BsonConverter()
        );
    }

    @Test
    public void testNetworkInterface() throws SocketException {
        List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
        Assert.assertNotNull(interfaces);
        for (NetworkInterface ifc : interfaces) {
            List<InterfaceAddress> addresses = ifc.getInterfaceAddresses();
            for (InterfaceAddress address : addresses) {

                System.out.printf("%s : %s(%d)\n", ifc.getDisplayName(), address.getAddress(), address.getNetworkPrefixLength());
                System.out.printf("Broadcast : %s\n", address.getBroadcast());
            }
        }
    }

    @Test
    public void converterSerDeserTest() throws ClassNotFoundException, IOException, InstantiationException, IllegalAccessException {
        for (Converter converter : converters) {
            Assert.assertTrue(testPrimitiveType(converter, 1.3, double.class));
            Assert.assertTrue(testPrimitiveType(converter, 1, int.class));
            Assert.assertTrue(testNumericObject(converter, 1.3f));
            Assert.assertTrue(testNumericObject(converter, 100L));
            Assert.assertTrue(testNumericObject(converter, 100));
            Assert.assertTrue(testNumericObject(converter, 1.3));
            Assert.assertTrue(testSimpleObject(converter));
            Assert.assertTrue(testGenericObject(converter));
            Assert.assertTrue(testComplexGeneric(converter));
        }
    }

    private <T> boolean testPrimitiveType(Converter converter, T v, Class<?> cls) throws ClassNotFoundException, InstantiationException, IllegalAccessException, IOException {
        Object resolved = converter.resolve(v, cls);
        return resolved.equals(v);
    }

    private <T> boolean testNumericObject(Converter converter, T v) throws ClassNotFoundException, InstantiationException, IllegalAccessException, IOException {
        T result = testObjectTransfer(converter, v, v.getClass());
        return v.equals(result);
    }

    private boolean testSimpleObject(Converter converter) throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException {
        User user = new User();
        user.setAge(30);
        User userBody = testObjectTransfer(converter, user, User.class);
        return userBody.equals(user);
    }

    public boolean testGenericObject(Converter converter) throws ClassNotFoundException, InstantiationException, IllegalAccessException, IOException {
        List<User> users = new LinkedList<>();
        User testUser = new User();
        testUser.setAge(30);
        testUser.setName("James");
        users.add(testUser);
        List<User> usersResult = testObjectTransfer(converter, users, Types.getType(List.class, User.class));
        return users.equals(usersResult);
    }

    private boolean testComplexGeneric(Converter converter) throws ClassNotFoundException, InstantiationException, IllegalAccessException, IOException {
        List<User> users = new ArrayList<>();
        User user = new User();
        user.setName("david");
        user.setAge(30);
        List<List<User>> userLists = new ArrayList<>();
        userLists.add(users);

        List<List<User>> userListResult = testObjectTransfer(converter, userLists, Types.getType(List.class, Types.getType(List.class, User.class)));
        return userListResult.equals(userLists);
    }

    private <T> T testObjectTransfer(Converter converter, T src, Type type) throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException {
        final Response response = Response.<T>success(src);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Writer writer = converter.writer(baos);
        writer.write(response);
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        Reader reader = converter.reader(bais);
        Response parsedResponse = reader.read(Response.class);
        if(Types.isCastable(parsedResponse.getBody(), type)) {
            return (T) parsedResponse.getBody();
        }
        return (T) converter.resolve(parsedResponse.getBody(), type);
    }
}
