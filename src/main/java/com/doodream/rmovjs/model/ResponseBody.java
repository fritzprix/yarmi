package com.doodream.rmovjs.model;


import com.google.gson.annotations.SerializedName;
import lombok.Data;

/**
 *  this class is used default response when {@link Response} is not used with type parameter.
 */
@Data
public class ResponseBody {
    @SerializedName("msg")
    String message;

    ResponseBody(String msg) {
        message = msg;
    }
}
