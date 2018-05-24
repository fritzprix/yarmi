package com.doodream.rmovjs.test.service;

import com.doodream.rmovjs.annotation.method.Put;
import com.doodream.rmovjs.annotation.parameter.Query;
import com.doodream.rmovjs.model.Response;
import lombok.NonNull;

public interface MessageController {

    @Put("/send")
    Response sendMessage(@Query(name = "content") @NonNull String msg);
}
