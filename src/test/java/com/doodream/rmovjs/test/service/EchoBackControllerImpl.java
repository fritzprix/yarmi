package com.doodream.rmovjs.test.service;

import com.doodream.rmovjs.model.Response;
import com.doodream.rmovjs.net.session.BlobSession;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class EchoBackControllerImpl implements EchoBackController {

    @Override
    public Response<String> sendMessage(String msg) {
        return Response.success(msg);
    }

    @Override
    public Response<User> sendJavaObject(User user) {
        return Response.success(user);
    }

    @Override
    public Response<Map<String, String>> sendMap(Map<String, String> map) {
        return Response.success(map);
    }

    @Override
    public Response<List<String>> sendList(List<String> list) {
        return Response.success(list);
    }

    @Override
    public Response<Set<String>> sendSet(Set<String> set) {
        return Response.success(set);
    }

    @Override
    public Response<List<User>> sendUserList(List<User> users) {
        return Response.success(users);
    }

    @Override
    public Response<Set<User>> sendUserSet(Set<User> users) {
        return Response.success(users);
    }

    @Override
    public Response<Map<String, User>> sendUserMap(Map<String, User> users) {
        return Response.success(users);
    }

    @Override
    public Response<ComplexObject> sendComplexObject(ComplexObject object) {
        return Response.success(object);
    }

    @Override
    public Response<List<ComplexObject>> sendComplexObjectList(List<ComplexObject> objects) {
        return Response.success(objects);
    }

    @Override
    public Response<Long> sendBlob(BlobSession data) {
        return null;
    }
}
