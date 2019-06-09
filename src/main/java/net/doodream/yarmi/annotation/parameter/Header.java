package net.doodream.yarmi.annotation.parameter;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Header {
    String name();

    boolean required() default true;
}
