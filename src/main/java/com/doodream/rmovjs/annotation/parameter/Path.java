package com.doodream.rmovjs.annotation.parameter;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by innocentevil on 18. 5. 4.
 */

@Retention(RetentionPolicy.RUNTIME)
public @interface Path {
    String name();

    boolean required() default true;
}
