package net.doodream.yarmi.test.service.echoback;

import net.doodream.yarmi.annotation.method.RMIExpose;
import net.doodream.yarmi.annotation.parameter.Body;
import net.doodream.yarmi.annotation.parameter.Query;
import net.doodream.yarmi.model.Response;
import net.doodream.yarmi.net.session.BlobSession;
import net.doodream.yarmi.test.data.ComplexObject;
import net.doodream.yarmi.test.data.User;

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
