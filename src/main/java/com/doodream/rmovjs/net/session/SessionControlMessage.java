package com.doodream.rmovjs.net.session;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class SessionControlMessage<T> {



    @SerializedName("cmd")
    private SessionCommand command = SessionCommand.RESET;
    @SerializedName("key")
    private String key = null;
    @SerializedName("param")
    private T param;

}
