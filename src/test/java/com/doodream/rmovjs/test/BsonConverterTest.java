package com.doodream.rmovjs.test;


import com.doodream.rmovjs.model.Response;
import com.doodream.rmovjs.serde.Converter;
import com.doodream.rmovjs.serde.Reader;
import com.doodream.rmovjs.serde.Writer;
import com.doodream.rmovjs.serde.bson.BsonConverter;
import com.doodream.rmovjs.test.service.User;
import com.doodream.rmovjs.util.Types;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class BsonConverterTest {




    @Test
    public void testNumericObject() throws ClassNotFoundException, InstantiationException, IllegalAccessException, IOException {
        Long id = 102L;
        Long result = testObjectTransfer(id, Long.class);
        Assert.assertEquals(id, result);
    }

    @Test
    public void testSimpleObject() throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException {
        User user = new User();
        user.setAge(30);
        User userBody = testObjectTransfer(user, User.class);
        Assert.assertEquals(userBody, user);
    }

    @Test
    public void testGenericObject() throws ClassNotFoundException, InstantiationException, IllegalAccessException, IOException {
        List<User> users = new LinkedList<>();
        User testUser = new User();
        testUser.setAge(30);
        testUser.setName("James");
        users.add(testUser);
        List<User> usersResult = testObjectTransfer(users, Types.getType(List.class, User.class));
        Assert.assertEquals(users, usersResult);
    }

    @Test
    public void testComplexGeneric() throws ClassNotFoundException, InstantiationException, IllegalAccessException, IOException {
        List<User> users = new ArrayList<>();
        User user = new User();
        user.setName("david");
        user.setAge(30);
        List<List<User>> userLists = new ArrayList<>();
        userLists.add(users);

        List<List<User>> userListResult = testObjectTransfer(userLists, Types.getType(List.class, Types.getType(List.class, User.class)));
        Assert.assertEquals(userListResult, userLists);
    }

    private <T> T testObjectTransfer(T src, Type type) throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException {
        final Response response = Response.<T>success(src);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Converter converter = new BsonConverter();
        Writer writer = converter.writer(baos);
        writer.write(response);
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        Reader reader = converter.reader(bais);
        Response parsedResponse = reader.read(Response.class);
        return (T) converter.resolve(parsedResponse.getBody(), type);
    }
}
