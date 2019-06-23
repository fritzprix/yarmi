package net.doodream.yarmi.data;

import com.doodream.cutils.Types;
import net.doodream.yarmi.serde.Converter;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

/**
 * Created by innocentevil on 18. 5. 4.
 * Param deals with serialization of parameters into String or vice-versa
 */

public class Param<T> {


    private int order;
    private T value;
    private transient Type type;


    public static class Builder<T> {
        private final Param<T> param = new Param<>();

        public Builder<T> type(Type type) {
            param.type = type;
            return this;
        }


        public Param<T> build() {
            return param;
        }
    }

    public static Param create(Type cls, Annotation[] annotations) {
        return Param.builder()
                .type(cls)
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
