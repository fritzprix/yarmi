package com.doodream.rmovjs.net.session;

import com.doodream.rmovjs.net.session.param.SCMErrorParam;

import java.util.Locale;

public class SessionControlException extends RuntimeException {
    SessionControlException(SCMErrorParam error) {
        super(String.format(Locale.ENGLISH, "Session Control exception : (%s) %s with argument : %d", error.getType(), error.getMsg(), error.getArg()));
    }
}
