package com.doodream.rmovjs.net.session;

import com.doodream.rmovjs.model.Response;
import com.doodream.rmovjs.net.session.param.SCMErrorParam;
import com.google.gson.annotations.SerializedName;

public class SessionControlMessage<T> {

    @SerializedName("cmd")
    private SessionCommand command = SessionCommand.RESET;
    @SerializedName("key")
    private String key = null;
    @SerializedName("param")
    private T param;


    private SessionControlMessage() { }

    public void setKey(String key) {
        this.key = key;
    }

    public void setCommand(SessionCommand command) {
        this.command = command;
    }

    public void setParam(T param) {
        this.param = param;
    }

    public static class Builder<T> {
        private final SessionControlMessage<T> message = new SessionControlMessage<>();
        private Builder() { }

        public Builder<T> key(String key) {
            message.key = key;
            return this;
        }

        public Builder<T> command(SessionCommand command) {
            message.command = command;
            return this;
        }

        public Builder<T> param(T param) {
            message.param = param;
            return this;
        }

        public SessionControlMessage<T> build() {
            return message;
        }
    }

    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    public String getKey() {
        return key;
    }

    public T getParam() {
        return param;
    }

    public SessionCommand getCommand() {
        return command;
    }
}
