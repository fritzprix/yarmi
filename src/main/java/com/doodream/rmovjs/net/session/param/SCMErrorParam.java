package com.doodream.rmovjs.net.session.param;

import com.doodream.rmovjs.net.session.SessionCommand;

public class SCMErrorParam {

    /**
     *  {@link SessionCommand} which caused error
     */

    private SessionCommand command;
    private String msg;
    private ErrorType type;

    static class Builder {
        private final SCMErrorParam error = new SCMErrorParam();

        private Builder() { }

        public Builder command(SessionCommand command) {
            error.command = command;
            return this;
        }

        public Builder msg(String msg) {
            error.msg = msg;
            return this;
        }


        public Builder type(ErrorType type) {
            error.type = type;
            return this;
        }

        public SCMErrorParam build() {
            return error;
        }
    }

    public static SCMErrorParam build(SessionCommand command, String msg, ErrorType type) {
        return SCMErrorParam.builder()
                .command(command)
                .msg(msg)
                .type(type)
                .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    private SCMErrorParam() { }


    public ErrorType getType() {
        return type;
    }

    public SessionCommand getCommand() {
        return command;
    }

    public String getMsg() {
        return msg;
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
