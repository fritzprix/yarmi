package com.doodream.rmovjs.test.service.echoback;

import com.doodream.rmovjs.annotation.method.RMIExpose;
import com.doodream.rmovjs.annotation.parameter.Body;
import com.doodream.rmovjs.annotation.parameter.Query;
import com.doodream.rmovjs.model.Response;
import com.doodream.rmovjs.net.session.BlobSession;
import com.doodream.rmovjs.test.data.ComplexObject;
import com.doodream.rmovjs.test.data.User;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface EchoBackController {

    @RMIExpose
    Response<String> sendMessage(@Query(name = "content") String msg);

    @RMIExpose
    Response<User> sendJavaObject(@Query(name = "user") User user);

    @RMIExpose
    Response<Map<String, String>> sendMap(@Query(name = "map") Map<String, String> map);

    @RMIExpose
    Response<List<String>> sendList(@Query(name = "list") List<String> list);

    @RMIExpose
    Response<Set<String>> sendSet(@Query(name = "set") Set<String> set);

    @RMIExpose
    Response<List<User>> sendUserList(@Query(name = "users") List<User> users);

    @RMIExpose
    Response<Set<User>> sendUserSet(@Query(name = "users") Set<User> users);

    @RMIExpose
    Response<Map<String, User>> sendUserMap(@Query(name = "users") Map<String, User> users);

    @RMIExpose
    Response<ComplexObject> sendComplexObject(@Query(name = "complex") ComplexObject object);

    @RMIExpose
    Response<List<ComplexObject>> sendComplexObjectList(@Query(name = "complexes") List<ComplexObject> objects);

    @RMIExpose
    Response<Long> sendBlob(@Body BlobSession data);

    @RMIExpose
    Response<Long> getBlobSize(@Query(name = "id") Long id);

}
