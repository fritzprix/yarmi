package com.doodream.rmovjs.annotation.method;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Deprecated
@Retention(RetentionPolicy.RUNTIME)
public @interface Post {
    String value();
}
