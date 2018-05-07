package com.doodream.rmovjs;

import lombok.Data;

@Data
public class Properties {

    public static String VERSION = "TEST_VERSION";
    public static String ARTIFACT_ID = "TEST_ARTIFACT";

    public static void load() {
        java.util.Properties props = new java.util.Properties();
        try {
            props.load(Properties.class.getResourceAsStream("project.properties"));
            Properties.VERSION = props.getProperty("version");
            Properties.ARTIFACT_ID = props.getProperty("artifactId");
        } catch (Exception ignore) {}
    }
}
