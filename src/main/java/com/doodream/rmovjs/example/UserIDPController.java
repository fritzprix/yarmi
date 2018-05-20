package com.doodream.rmovjs.example;

import com.doodream.rmovjs.annotation.method.Get;
import com.doodream.rmovjs.annotation.method.Post;
import com.doodream.rmovjs.annotation.parameter.Body;
import com.doodream.rmovjs.annotation.parameter.Path;
import com.doodream.rmovjs.model.Response;

public interface UserIDPController {

    @Get("/{id}")
    Response<User> getUser(@Path(name = "id") Long userId);

    @Get("/list")
    Response<User> getUsers();

    @Post("/new")
    Response<User> createUser(@Body(name = "user") User user);

}
