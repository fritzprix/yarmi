package com.doodream.rmovjs.net.session.param;

import com.doodream.rmovjs.net.session.SessionCommand;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class SCMReasonParam {

    private SessionCommand command;
    private String msg;

    public static SCMReasonParam build(SessionCommand command, String msg) {
        return SCMReasonParam.builder().command(command).msg(msg).build();
    }
}
