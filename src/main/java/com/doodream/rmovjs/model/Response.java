package com.doodream.rmovjs.model;

import com.google.common.base.Preconditions;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created by innocentevil on 18. 5. 4.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Response<T> {
    public static final int SUCCESS = 200;

    String endpoint;
    T body;
    boolean isSuccessful;
    ResponseBody errorBody;
    int code;

    public static <T> Response<T> success(T body) {
        return Response.<T>builder()
                .body(body)
                .code(SUCCESS)
                .isSuccessful(true)
                .build();
    }

    public static Response error(int code, String mesg) {
        return Response.<ResponseBody>builder()
                .code(code)
                .isSuccessful(false)
                .errorBody(new ResponseBody(mesg))
                .build();
    }

    public static Response success(String msg) {
        return Response.builder()
                .code(SUCCESS)
                .isSuccessful(true)
                .body(new ResponseBody(msg))
                .build();
    }

    public static Response from(RMIError error) {
        return error.getResponse();
    }

    public static void validate(Response res) {
        if(res.code == Response.SUCCESS) {
            Preconditions.checkNotNull(res.getBody(), "Successful response must have non-null body");
        } else {
            Preconditions.checkNotNull(res.getErrorBody(), "Error response must have non-null error body");
        }
    }
}
