package net.doodream.yarmi.net.session;

import net.doodream.yarmi.net.session.param.SCMChunkParam;
import net.doodream.yarmi.net.session.param.SCMErrorParam;

public enum SessionCommand {
    CHUNK(SCMChunkParam.class),
    ACK(null),
    RESET(null),
    ERR(SCMErrorParam.class);

    private final Class<?> paramCls;

    SessionCommand(Class<?> parameterCls) {
        paramCls = parameterCls;
    }

    public static SessionCommand fromString(String cmd) {
        String upperCaseCmd = cmd.toUpperCase();
        switch (upperCaseCmd) {
            case "CHUNK":
                return CHUNK;
            case "ACK":
                return ACK;
            case "RESET":
                return RESET;
            case "ERR":
                return ERR;
            default:
                throw new RuntimeException("Unknown Command : " + cmd);
        }
    }

    public Class getParamClass() {
        return paramCls;
    }
}
