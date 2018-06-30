package com.doodream.rmovjs.net.session.param;

import com.doodream.rmovjs.net.session.SessionCommand;
import com.google.gson.Gson;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class SCMErrorParam {

    public static SCMErrorParam build(SessionCommand command, String msg, int code) {
        return null;
    }
    private static final Gson GSON = new Gson();

    private SessionCommand command;
    private String msg;
    private int arg;
    private ErrorType type;


    public enum ErrorType {
        BAD_SEQUENCE(-1001) {
            @Override
            int[] parse(SCMErrorParam errorParam) {
                return new int[0];
            }
        };

        final int code;
        ErrorType(int code) {
            this.code = code;
        }

        abstract int[] parse(SCMErrorParam errorParam);


    }

    public static SCMErrorParam buildUnsupportedOperation(SessionCommand command) {
        return null;
    }
}
