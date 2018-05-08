package com.doodream.rmovjs.annotation.server;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Controller {
    String path();
    int version();
    Class module();
}
