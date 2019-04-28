package com.doodream.rmovjs;

import lombok.Data;

@Data
public class Properties {

    private static final java.util.Properties PROPERTIES;
    static {
        PROPERTIES = new java.util.Properties();
        try {
            PROPERTIES.load(Properties.class.getClassLoader().getResourceAsStream("project.properties"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getHealthCheckPath() {
        return PROPERTIES.getProperty("healthCheckPath","");
    }

    public static String getArtifactId() {
        return PROPERTIES.getProperty("artifactId", "yarmi-core");
    }

    public static String getVersionString() {
        return PROPERTIES.getProperty("version", "0.0.1");
    }

    public static Integer getMaxIOParallelism() {
        return Integer.valueOf(PROPERTIES.getProperty("io.thread.pool.size", "4"));
    }
}
