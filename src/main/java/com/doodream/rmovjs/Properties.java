package com.doodream.rmovjs;

import lombok.Data;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.Configurator;

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

            ConfigurationSource log4jConfig = ConfigurationSource.fromResource("log4j.xml", Properties.class.getClassLoader());
            Configurator.initialize(null, log4jConfig);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
