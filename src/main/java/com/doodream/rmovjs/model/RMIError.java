package com.doodream.rmovjs.model;

/**
 *
 */
public enum RMIError {
    NOT_FOUND(404, Response.error(404, "Not Found")),
    NOT_IMPLEMENTED(501, Response.error(501,"Not implemented method")),
    FORBIDDEN(403, Response.error(403, "ServiceInfo is not matched")),
    BAD_REQUEST(400, Response.error(400, "Bad Request")),
    UNHANDLED(400, Response.error(400, "Request Not Handled")),
    INTERNAL_SERVER_ERROR(500, Response.error(500,"Internal Server Error")),
    TIMEOUT(500, Response.error(500,"Timeout")),
    BAD_RESPONSE(501, Response.error(501,"Bad Response"));

    private final int code;
    private final Response<String> response;
    RMIError(int code, Response<String> response) {
        this.code = code;
        this.response = response;
    }

    public Response getResponse() {
        return response;
    }

    public int code() {
        return code;
    }

}
