package com.doodream.rmovjs.model;


import com.google.gson.annotations.SerializedName;

/**
 *  this class is used default response when {@link Response} is not used with type parameter.
 */
public class ResponseBody {
    @SerializedName("msg")
    private String message;

    ResponseBody(String msg) {
        message = msg;
    }
}
