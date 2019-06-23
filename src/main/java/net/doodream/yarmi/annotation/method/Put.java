package net.doodream.yarmi.annotation.method;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
@Deprecated
@Retention(RetentionPolicy.RUNTIME)
public @interface Put {
    String value();
}
