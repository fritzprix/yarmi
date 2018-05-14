package com.doodream.rmovjs.annotation.server;


import com.doodream.rmovjs.annotation.parameter.KeyValue;
import com.doodream.rmovjs.net.ServiceAdapter;
import com.doodream.rmovjs.net.inet.InetServiceAdapter;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by innocentevil on 18. 5. 4.
 */

@Retention(RetentionPolicy.RUNTIME)
public @interface Service {
    String name();
    Class<? extends ServiceAdapter> adapter() default InetServiceAdapter.class;
    String[] params() default {};
}
