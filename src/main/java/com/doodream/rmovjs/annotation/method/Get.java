package com.doodream.rmovjs.annotation.method;

import com.doodream.rmovjs.method.RMIMethod;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by innocentevil on 18. 5. 4.
 */

@Retention(RetentionPolicy.RUNTIME)
public @interface Get {
    String value();
}
