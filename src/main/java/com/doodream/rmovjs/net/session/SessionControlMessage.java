package com.doodream.rmovjs.net.session;

import com.google.gson.TypeAdapter;
import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import jdk.nashorn.internal.runtime.arrays.TypedArrayData;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.IOException;

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
