package com.doodream.rmovjs.model;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Builder
@Data
public class ServiceInfo {
    @SerializedName("name")
    private String name;
    @SerializedName("version")
    private String version;
    @SerializedName("services")
    private List<Class> services;
}
