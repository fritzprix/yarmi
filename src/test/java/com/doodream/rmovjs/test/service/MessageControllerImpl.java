package com.doodream.rmovjs.test.service;

import com.doodream.rmovjs.model.Response;

public class MessageControllerImpl implements MessageController {

    @Override
    public Response sendMessage(String msg) {
        return Response.success(msg);
    }
}
