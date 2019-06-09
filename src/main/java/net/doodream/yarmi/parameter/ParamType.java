package net.doodream.yarmi.parameter;

import net.doodream.yarmi.annotation.parameter.Body;
import net.doodream.yarmi.annotation.parameter.Header;
import net.doodream.yarmi.annotation.parameter.Path;
import net.doodream.yarmi.annotation.parameter.Query;

import java.lang.annotation.Annotation;

/**
 * Created by innocentevil on 18. 5. 4.
 */

public enum ParamType {
    QUERY(Query.class),
    PATH(Path.class),
    BODY(Body.class),
    HEADER(Header.class);

    private final Class cls;

    ParamType(Class cls) {
        this.cls = cls;
    }

    public static boolean isSupportedAnnotation(Annotation annotation) {
        return QUERY.isTypeOf(annotation) ||
                PATH.isTypeOf(annotation) ||
                HEADER.isTypeOf(annotation) ||
                BODY.isTypeOf(annotation);
    }


    public static ParamType fromAnnotation(Annotation annotation) throws IllegalArgumentException {
        if(QUERY.isTypeOf(annotation)) {
            return QUERY;
        } else if(PATH.isTypeOf(annotation)) {
            return PATH;
        } else if(HEADER.isTypeOf(annotation)) {
            return HEADER;
        } else if(BODY.isTypeOf(annotation)){
            return BODY;
        }
        throw new IllegalArgumentException("Annotation is not supported");
    }

    private boolean isTypeOf(Annotation annotation) {
        return this.cls.getName().equalsIgnoreCase(annotation.annotationType().getName());
    }

    public String getName(Annotation annotation) {
        switch (this){
            case HEADER:
                return ((Header) annotation).name();
            case BODY:
                return annotation.toString();
            case PATH:
                return ((Path) annotation).name();
            case QUERY:
                return ((Query) annotation).name();
            default:
                throw new IllegalArgumentException("Annotation is not supported");
        }
    }

    public boolean isRequired(Annotation annotation) {
        switch (this) {
            case HEADER:
                return ((Header) annotation).required();
            case BODY:
                return ((Body) annotation).required();
            case PATH:
                return ((Path) annotation).required();
            case QUERY:
                return ((Query) annotation).required();
            default:
                throw new IllegalArgumentException("Annotation is not supported");
        }
    }

}
