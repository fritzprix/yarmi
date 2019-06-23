package net.doodream.yarmi.data;

/**
 *
 */
public enum RMIError {
    NOT_FOUND(404, Response.error(404, "Not Found")),
    NOT_IMPLEMENTED(501, Response.error(501,"Not implemented method")),
    FORBIDDEN(403, Response.error(403, "ServiceInfo is not matched")),
    DUPLICATE(405, Response.error(405, "Duplicate Request")),
    BAD_REQUEST(400, Response.error(400, "Bad Request")),
    UNHANDLED(400, Response.error(400, "Request Not Handled")),
    INTERNAL_SERVER_ERROR(500, Response.error(500,"Internal Server Error")),
    TIMEOUT(500, Response.error(500,"Timeout")),
    BAD_RESPONSE(501, Response.error(501,"Bad Response")),
    CLOSED(510, Response.error(510, "RMI channel close"));

    private final int code;
    private final Response<String> response;
    RMIError(int code, Response<String> response) {
        this.code = code;
        this.response = response;
    }

    public static boolean isServiceBad(int code) {
        return code >= 500;
    }

    public Response getResponse() {
        return response;
    }

    public int code() {
        return code;
    }

}
