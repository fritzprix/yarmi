package net.doodream.yarmi.test.service.echoback;

import net.doodream.yarmi.data.Response;
import net.doodream.yarmi.net.session.BlobSession;
import net.doodream.yarmi.net.session.Session;
import net.doodream.yarmi.test.data.ComplexObject;
import net.doodream.yarmi.test.data.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class EchoBackControllerImpl implements EchoBackController {
    private static final Logger Log = LoggerFactory.getLogger(EchoBackControllerImpl.class);
    private volatile long binarySize = 0L;

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
        Session session = null;
        Log.debug("send blob");
        try {
            session = data.open();
            byte[] rbuf = new byte[1 << 16];
            int sz;
            binarySize = 0;
            while((sz = session.read(rbuf,0, rbuf.length)) > 0) {
                binarySize += sz;
                Log.debug("sz {} / {}", sz, binarySize);

            }
        } catch (IOException ignored) { } finally {
            if(session != null) {
                try {
                    session.close();
                } catch (IOException ignored) { }
            }
        }
        return Response.success(0L);
    }

    @Override
    public Response<Long> getBlobSize(Long id) {
        Log.debug("val : {}", binarySize);
        return Response.success(binarySize);
    }


}
