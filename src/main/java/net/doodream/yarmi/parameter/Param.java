package net.doodream.yarmi.parameter;

import net.doodream.yarmi.serde.Converter;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import com.doodream.cutils.Types;

/**
 * Created by innocentevil on 18. 5. 4.
 * Param deals with serialization of parameters into String or vice-versa
 */

public class Param<T> {


    private int order;
    private ParamType location;
    private boolean required;
    private String name;
    private T value;
    private transient Type type;


    public static class Builder<T> {
        private final Param<T> param = new Param<>();

        public Builder<T> type(Type type) {
            param.type = type;
            return this;
        }

        public Builder<T> location(ParamType paramLocation) {
            param.location = paramLocation;
            return this;
        }

        public Builder<T> name(String name) {
            param.name = name;
            return this;
        }

        public Builder<T> required(boolean required) {
            param.required = required;
            return this;
        }

        public Param<T> build() {
            return param;
        }
    }

    public static Param create(Type cls, Annotation[] annotations) {
        List<Annotation> validAnnotation = new ArrayList<>();
        for (Annotation annotation : annotations) {
            if(ParamType.isSupportedAnnotation(annotation)) {
                validAnnotation.add(annotation);
            }
        }

        Annotation annotation = validAnnotation.get(0);
        ParamType type = ParamType.fromAnnotation(annotation);

        return Param.builder()
                .type(cls)
                .location(type)
                .name(type.getName(annotation))
                .required(type.isRequired(annotation))
                .build();
    }

    private static Builder builder() {
        return new Builder();
    }

    public static int sort(Param p1, Param p2) {
        return p1.order - p2.order;
    }

    public void apply(T value) {
        this.value = value;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public Object resolve(Converter converter, Type type) throws IllegalAccessException, InstantiationException, ClassNotFoundException {
        if(Types.isCastable(value, type)) {
            return value;
        }
        if(Types.isPrimitive(type)) {
            return Types.primitive(value, type);
        }
        return converter.resolve(value, type);
    }

    public boolean isInstanceOf(Type itfc) {
        return this.type == itfc;
    }
}
