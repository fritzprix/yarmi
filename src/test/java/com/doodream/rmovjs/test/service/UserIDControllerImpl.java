package com.doodream.rmovjs.test.service;

import com.doodream.rmovjs.model.Response;

import java.util.List;

public class UserIDControllerImpl implements UserIDPController {
    @Override
    public Response<User> getUser(Long userId) {
        return null;
    }

    @Override
    public Response<List<User>> getUsers(List<Long> ids, boolean asc) {
        return null;
    }

    @Override
    public Response<User> createUser(User user) {
        return null;
    }

}
