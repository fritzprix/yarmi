package com.doodream.rmovjs.annotation.parameter;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface AdapterParam {
    String key();
    String value();
}
