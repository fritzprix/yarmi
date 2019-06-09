package net.doodream.yarmi.model;



/**
 *  this class is used default response when {@link Response} is not used with type parameter.
 */
public class ResponseBody {
    private String message;

    ResponseBody(String msg) {
        message = msg;
    }
}
