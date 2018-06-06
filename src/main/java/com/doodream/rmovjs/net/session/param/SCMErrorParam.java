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
public class SCMErrorParam {

    private SessionCommand command;
    private String msg;

    public static SCMErrorParam build(SessionCommand command, String msg) {
        return SCMErrorParam.builder().command(command).msg(msg).build();
    }
}
