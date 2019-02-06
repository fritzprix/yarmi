package com.doodream.rmovjs.test.service;

import com.doodream.rmovjs.annotation.method.Get;
import com.doodream.rmovjs.annotation.method.Post;
import com.doodream.rmovjs.annotation.parameter.Body;
import com.doodream.rmovjs.annotation.parameter.Path;
import com.doodream.rmovjs.annotation.parameter.Query;
import com.doodream.rmovjs.model.Response;
import com.doodream.rmovjs.net.session.BlobSession;

import java.util.List;

public interface UserIDPController {

    @Get("/{id}")
    Response<User> getUser(@Path(name = "id") Long userId);

    @Get("/list")
    Response<List<User>> getUsers(@Query(name = "ids") List<Long> ids, @Query(name = "asc") boolean asc);

    @Post("/new")
    Response<User> createUser(@Body(name = "user") User user);

    @Post("/bad/request1")
    Response<BlobSession> getUserData(@Body(name = "id") long id);

//    @Post("/bad/request2")
//    Response<User> badMethod2(@Body(name = "data") BlobSession data, @Body(name = "data2") BlobSession data2);

}
