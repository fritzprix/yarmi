package com.doodream.rmovjs.example.server;

import com.doodream.rmovjs.example.template.User;
import com.doodream.rmovjs.example.template.UserIDPController;
import com.doodream.rmovjs.model.Response;

import java.util.HashMap;

public class UserIDControllerImpl implements UserIDPController {

    private HashMap<Long, User> userTable = new HashMap<>();

    @Override
    public Response<User> getUser(Long userId) {
        User user = userTable.get(userId);
        if(user == null) {
            return Response.error(400, "Invalid User Id");
        }
        return Response.success(user, User.class);
    }

    @Override
    public Response<User> getUsers() {
        return null;
    }

    @Override
    public Response<User> createUser(User user) {
        int id = user.hashCode();
        userTable.put((long) id, user);
        user.setId((long) id);
        return Response.success(user, User.class);
    }
}
