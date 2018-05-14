package com.doodream.rmovjs.model;

import com.doodream.rmovjs.Properties;
import com.doodream.rmovjs.annotation.server.Service;
import com.doodream.rmovjs.net.inet.InetServiceAdapter;
import com.doodream.rmovjs.server.RMIController;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import io.reactivex.Observable;
import lombok.Builder;
import lombok.Data;

import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.util.Arrays;
import java.util.List;

@Builder
@Data
public class RMIServiceInfo {

    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(Class.class, new TypeAdapter<Class>() {
                @Override
                public void write(JsonWriter jsonWriter, Class aClass) throws IOException {
                    jsonWriter.value(aClass.getCanonicalName());
                }

                @Override
                public Class read(JsonReader jsonReader) throws IOException {
                    try {
                        return Class.forName(jsonReader.nextString());
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                    return null;
                }
            }).create();

    @SerializedName("name")
    private String name;

    @SerializedName("version")
    private String version;

    @SerializedName("adapter")
    private Class adapter;

    @SerializedName("params")
    private List<String> params;

    @SerializedName("interfaces")
    private List<ControllerInfo> controllerInfos;

    //TODO :


    public static <T> RMIServiceInfo from(Class<T> svc) {
        Service service = svc.getAnnotation(Service.class);
        RMIServiceInfoBuilder builder = RMIServiceInfo.builder();

        builder.version(Properties.VERSION)
                .adapter(service.adapter())
                .params(Arrays.asList(service.params()))
                .name(service.name());

        Observable.fromArray(svc.getDeclaredFields())
                .filter(RMIController::isValidController)
                .map(RMIController::create)
                .map(ControllerInfo::build)
                .toList()
                .doOnSuccess(builder::controllerInfos)
                .subscribe();

        return builder.build();

    }

    public String toJson() {
        return GSON.toJson(this);
    }

    public static RMIServiceInfo from(byte[] bytes) throws UnsupportedEncodingException {
        JsonReader reader = new JsonReader(new StringReader(new String(bytes, "UTF-8")));
        reader.setLenient(true);
        return GSON.fromJson(reader, RMIServiceInfo.class);
    }
}
