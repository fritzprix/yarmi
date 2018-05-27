package com.doodream.rmovjs.model;


import com.doodream.rmovjs.net.ClientSocketAdapter;
import com.doodream.rmovjs.parameter.Param;
import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class Request {

    private transient ClientSocketAdapter client;

    @SerializedName("endpoint")
    private String endpoint;

    @SerializedName("params")
    private List<Param> params;

    // TODO : blob header & blob

    public static boolean isValid(Request request) {
        return (request.getEndpoint() != null) &&
                (request.getParams() != null);
    }

}
