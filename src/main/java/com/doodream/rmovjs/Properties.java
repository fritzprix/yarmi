package com.doodream.rmovjs;

import lombok.Data;

@Data
public class Properties {

    public static String VERSION = "TEST_VERSION";
    public static String ARTIFACT_ID = "TEST_NAME";
    static {
        java.util.Properties props = new java.util.Properties();
        try {
            props.load(Properties.class.getClassLoader().getResourceAsStream("project.properties"));
            Properties.VERSION = props.getProperty("version");
            Properties.ARTIFACT_ID = props.getProperty("artifactId");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
