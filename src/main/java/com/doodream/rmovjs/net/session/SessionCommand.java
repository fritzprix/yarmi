package com.doodream.rmovjs.net.session;

import com.doodream.rmovjs.net.session.param.SCMChunkParam;
import com.doodream.rmovjs.net.session.param.SCMEchoParam;
import com.doodream.rmovjs.net.session.param.SCMReasonParam;

public enum SessionCommand {
    CHUNK(SCMChunkParam.class),
    ACK(SCMReasonParam.class),
    RESET(SCMReasonParam.class),
    ERR(SCMReasonParam.class),
    ECHO(SCMEchoParam.class),
    ECHOBACK(SCMEchoParam.class);

    private final Class<?> paramCls;
    SessionCommand(Class<?> pcls) {
        paramCls = pcls;
    }
}
