package com.doodream.rmovjs.model;


import com.doodream.rmovjs.method.RMIMethod;
import com.doodream.rmovjs.net.ClientSocketAdapter;
import com.doodream.rmovjs.parameter.Param;
import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.util.List;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class Request {

    private transient ClientSocketAdapter client;
    @SerializedName("endpoint")
    private Endpoint endpoint;



    public void response(Response s) throws IOException {
        if(client == null) {
            throw new IOException("No Client");
        }
        client.write(s);
    }

    public final RMIMethod getMethodType() {
        return endpoint.method;
    }

    public final String getPath() {
        return endpoint.path;
    }

    public final List<Param> getParameters() {
        return endpoint.params;
    }

    public static boolean isValid(Request request) {
        return (request.getEndpoint() != null) &&
                (request.getPath() != null) &&
                (request.getParameters() != null) &&
                (request.getMethodType() != null);
    }

}
