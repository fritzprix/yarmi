package com.doodream.rmovjs.annotation;

import com.doodream.rmovjs.model.Response;

public class RMIException extends RuntimeException {
    private int code;

    public RMIException(Response response) {
        super((String) response.getBody());
        code = response.getCode();
    }

    public int code() {
        return code;
    }
}
