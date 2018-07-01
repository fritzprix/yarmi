package com.doodream.rmovjs.net.session;

import com.doodream.rmovjs.net.session.param.SCMErrorParam;

import java.util.Locale;

public class SessionControlException extends RuntimeException {
    /**
     * create {@link SessionControlException} from {@link SCMErrorParam}
     * @param error {@link SCMErrorParam} object used to build {@link SessionControlException}
     */
    SessionControlException(SCMErrorParam error) {
        super(String.format(Locale.ENGLISH, "Session Control exception : request = %s / type : %s  / message %s with argument ", error.getCommand(), error.getType(), error.getMsg()));
    }
}
