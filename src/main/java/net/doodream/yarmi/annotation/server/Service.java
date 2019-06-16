package net.doodream.yarmi.annotation.server;


import net.doodream.yarmi.annotation.parameter.AdapterParam;
import net.doodream.yarmi.net.DefaultNegotiator;
import net.doodream.yarmi.net.Negotiator;
import net.doodream.yarmi.net.ServiceAdapter;
import net.doodream.yarmi.net.tcp.TcpServiceAdapter;
import net.doodream.yarmi.serde.Converter;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Service Annotation
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


    String provider();


    /**
     * class of network adapter implementation upon which service depend.
     * @return network adapter class
     */
    Class<? extends ServiceAdapter> adapter() default TcpServiceAdapter.class;

    /**
     * parameters for constructor of network adapter class, will be passed as argument whenever the adapter class
     * needs to be instantiated.
     * the order of parameters will be kept in the process of ser-der.
     * @return parameters to adapter constructor
     */
    AdapterParam[] params() default {};

    Class<? extends Negotiator> negotiator() default DefaultNegotiator.class;

    Class<? extends Converter> converter();
}
