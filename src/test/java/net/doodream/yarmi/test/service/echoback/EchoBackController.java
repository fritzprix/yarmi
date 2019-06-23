package net.doodream.yarmi.test.service.echoback;

import net.doodream.yarmi.annotation.RMIExpose;
import net.doodream.yarmi.data.Response;
import net.doodream.yarmi.net.session.BlobSession;
import net.doodream.yarmi.test.data.ComplexObject;
import net.doodream.yarmi.test.data.User;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface EchoBackController {

    @RMIExpose
    Response<String> sendMessage(String msg);

    @RMIExpose
    Response<User> sendJavaObject(User user);

    @RMIExpose
    Response<Map<String, String>> sendMap(Map<String, String> map);

    @RMIExpose
    Response<List<String>> sendList(List<String> list);

    @RMIExpose
    Response<Set<String>> sendSet(Set<String> set);

    @RMIExpose
    Response<List<User>> sendUserList(List<User> users);

    @RMIExpose
    Response<Set<User>> sendUserSet(Set<User> users);

    @RMIExpose
    Response<Map<String, User>> sendUserMap(Map<String, User> users);

    @RMIExpose
    Response<ComplexObject> sendComplexObject(ComplexObject object);

    @RMIExpose
    Response<List<ComplexObject>> sendComplexObjectList(List<ComplexObject> objects);

    @RMIExpose
    Response<Long> sendBlob(BlobSession data);

    @RMIExpose
    Response<Long> getBlobSize(Long id);

}
