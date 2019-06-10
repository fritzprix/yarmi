package net.doodream.yarmi.annotation.method;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by innocentevil on 18. 5. 4.
 */

@Deprecated
@Retention(RetentionPolicy.RUNTIME)
public @interface Get {
    String value();
}
