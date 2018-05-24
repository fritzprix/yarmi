package com.doodream.rmovjs.model;

/**
 *
 */
public enum RMIError {
    NOT_FOUND(404, Response.error(404, "Not Found")),
    NOT_IMPLEMENTED(500, Response.error(501,"Not implemented method")),
    FORBIDDEN(403, Response.error(403, "ServiceInfo is not matched")),
    BAD_REQUEST(400, Response.error(400, "Bad Request"));

    private final int code;
    private final Response response;
    RMIError(int code, Response response) {
        this.code = code;
        this.response = response;
    }

    Response getResponse() {
        return response;
    }
}
