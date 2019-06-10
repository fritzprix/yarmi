package net.doodream.yarmi.annotation.server;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Controller {
    String path();
    int version();
    Class module();
}
