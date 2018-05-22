package com.doodream.rmovjs.model;

/**
 *
 */
public enum RMIError {
    NOT_FOUND(404, Response.error(404, "Not Found")),
    FORBIDDEN(403, Response.error(403, "serviceInfo is not matched"));

    private final int code;
    private final Response response;
    RMIError(int code, Response response) {
        this.code = code;
        this.response = response;
    }

    public Response getResponse(Request request) {
        Response nresponse = Response.from(response);
        nresponse.setEndpoint(request.getEndpoint());
        return nresponse;
    }

}
