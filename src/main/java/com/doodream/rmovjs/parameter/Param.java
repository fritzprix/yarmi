package com.doodream.rmovjs.parameter;

import com.doodream.rmovjs.model.Response;
import com.doodream.rmovjs.serde.Converter;
import com.doodream.rmovjs.util.Types;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.reactivex.Observable;
import io.reactivex.functions.Predicate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;

/**
 * Created by innocentevil on 18. 5. 4.
 * Param deals with serialization of parameters into String or vice-versa
 */

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class Param<T> {

    private static final Gson GSON = new GsonBuilder().create();
    private static final Logger Log = LoggerFactory.getLogger(Param.class);

    private int order;
    private ParamType location;
    private boolean required;
    private String name;
    private T value;
    private transient Type type;

    public static Param create(Type cls, Annotation[] annotations) {
        List<Annotation> filteredAnnotations = Observable.fromArray(annotations)
                .filter(new Predicate<Annotation>() {
                    @Override
                    public boolean test(Annotation annotation) throws Exception {
                        return ParamType.isSupportedAnnotation(annotation);
                    }
                })
                .toList()
                .blockingGet();

        Annotation annotation = filteredAnnotations.get(0);
        ParamType type = ParamType.fromAnnotation(annotation);

        return Param.builder()
                .type(cls)
                .location(type)
                .name(type.getName(annotation))
                .required(type.isRequired(annotation))
                .build();
    }

    public static int sort(Param p1, Param p2) {
        return p1.order - p2.order;
    }

    public void apply(T value) {
        this.value = value;
    }

    public Object resolve(Converter converter, Type type) throws IllegalAccessException, InstantiationException, ClassNotFoundException {
        if(Types.isCastable(value, type)) {
            return (T) value;
        }
        return converter.resolve(value, type);
    }

    public boolean isInstanceOf(Type itfc) {
        return this.type == itfc;
    }
}
