package net.doodream.yarmi.annotation;

import net.doodream.yarmi.data.Response;

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
