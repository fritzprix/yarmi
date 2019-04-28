package com.doodream.rmovjs.test.service.echoback;

import com.doodream.rmovjs.annotation.method.Get;
import com.doodream.rmovjs.annotation.method.Put;
import com.doodream.rmovjs.annotation.parameter.Body;
import com.doodream.rmovjs.annotation.parameter.Query;
import com.doodream.rmovjs.model.Response;
import com.doodream.rmovjs.net.session.BlobSession;
import com.doodream.rmovjs.test.data.ComplexObject;
import com.doodream.rmovjs.test.data.User;
import lombok.NonNull;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface EchoBackController {

    @Put("/send/message")
    Response<String> sendMessage(@Query(name = "content") @NonNull String msg);

    @Put("/send/user")
    Response<User> sendJavaObject(@Query(name = "user") @NonNull User user);

    @Put("/send/generic/map")
    Response<Map<String, String>> sendMap(@Query(name = "map") @NonNull Map<String, String> map);

    @Put("/send/generic/list")
    Response<List<String>> sendList(@Query(name = "list") @NonNull List<String> list);

    @Put("/send/generic/set")
    Response<Set<String>> sendSet(@Query(name = "set") @NonNull Set<String> set);

    @Put("/send/list/user")
    Response<List<User>> sendUserList(@Query(name = "users") List<User> users);

    @Put("/send/set/user")
    Response<Set<User>> sendUserSet(@Query(name = "users") Set<User> users);

    @Put("/send/map/user")
    Response<Map<String, User>> sendUserMap(@Query(name = "users") Map<String, User> users);

    @Put("/send/complex")
    Response<ComplexObject> sendComplexObject(@Query(name = "complex") ComplexObject object);

    @Put("/send/list/complex")
    Response<List<ComplexObject>> sendComplexObjectList(@Query(name = "complexes") List<ComplexObject> objects);

    @Put("/send/data")
    Response<Long> sendBlob(@Body(name = "data") BlobSession data);

    @Get("/data/size")
    Response<Long> getBlobSize(@Query(name = "id") Long id);

}
