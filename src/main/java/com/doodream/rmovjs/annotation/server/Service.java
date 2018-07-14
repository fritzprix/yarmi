package com.doodream.rmovjs.annotation.server;


import com.doodream.rmovjs.net.RMINegotiator;
import com.doodream.rmovjs.net.ServiceAdapter;
import com.doodream.rmovjs.net.SimpleNegotiator;
import com.doodream.rmovjs.net.tcp.TcpServiceAdapter;
import com.doodream.rmovjs.serde.Converter;
import com.doodream.rmovjs.serde.json.JsonConverter;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by innocentevil on 18. 5. 4.
 */


/**
 * Service Annotation
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Service {
    /**
     * the name of service
     * @return name of service
     */
    String name();

    /**
     * class of network adapter implementation upon which service depend.
     * @return network adapter class
     */
    Class<? extends ServiceAdapter> adapter() default TcpServiceAdapter.class;

    /**
     * parameters for constrcutor of network adapter class, will be passed as argument whenever the adapter class
     * needs to be instanciated.
     * the order of parameters will be kept in the process of ser-der.
     * @return parameters to adapter constructor
     */
    String[] params() default {};

    Class<? extends RMINegotiator> negotiator() default SimpleNegotiator.class;

    Class<? extends Converter> converter() default JsonConverter.class;
}
