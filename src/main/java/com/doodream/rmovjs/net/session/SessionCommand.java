package com.doodream.rmovjs.net.session;

import com.doodream.rmovjs.net.session.param.SCMChunkParam;
import com.doodream.rmovjs.net.session.param.SCMEchoParam;
import com.doodream.rmovjs.net.session.param.SCMErrorParam;

public enum SessionCommand {
    CHUNK(SCMChunkParam.class),
    ACK(null),
    RESET(SCMErrorParam.class),
    ERR(SCMErrorParam.class),
    ECHO(SCMEchoParam.class),
    ECHO_BACK(SCMEchoParam.class);

    private final Class<?> paramCls;
    SessionCommand(Class<?> parameterCls) {
        paramCls = parameterCls;
    }
}
