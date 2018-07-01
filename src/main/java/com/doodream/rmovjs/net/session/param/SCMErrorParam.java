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

    private static final Gson GSON = new Gson();

    /**
     *  {@link SessionCommand} which caused error
     */

    private SessionCommand command;
    private String msg;
    private ErrorType type;

    public static SCMErrorParam build(SessionCommand command, String msg, ErrorType type) {
        return SCMErrorParam.builder()
                .command(command)
                .msg(msg)
                .type(type)
                .build();
    }


    public enum ErrorType {
        BAD_SEQUENCE(-1),
        INVALID_SESSION(-2),INVALID_OP(-3);

        final int code;
        ErrorType(int code) {
            this.code = code;
        }



    }

    public static SCMErrorParam buildUnsupportedOperation(SessionCommand command) {
        return null;
    }
}
