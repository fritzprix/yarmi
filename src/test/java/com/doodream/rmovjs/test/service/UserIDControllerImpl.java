package com.doodream.rmovjs.test.service;

import com.doodream.rmovjs.model.Response;
import com.doodream.rmovjs.net.session.BlobSession;

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

    @Override
    public Response<BlobSession> getUserData(long id) {
        return null;
    }

//    @Override
//    public Response<BlobSession> badMethod1(BlobSession data) {
//        return null;
//    }

//    @Override
//    public Response<User> badMethod2(BlobSession data, BlobSession data2) {
//        return null;
//    }


}
