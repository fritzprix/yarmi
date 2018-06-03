package com.doodream.rmovjs.parameter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.reactivex.Observable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
public class Param {

    private static final Gson GSON = new GsonBuilder().create();
    private static final Logger Log = LogManager.getLogger(Param.class);

    private int order;
    private ParamType type;
    private boolean required;
    private String name;
    private String value;
    private Class cls;

    public static Param create(Class paramCls, Annotation[] annotations) {
        List<Annotation> filteredAnnotations = Observable.fromArray(annotations)
                .filter(ParamType::isSupportedAnnotation)
                .toList()
                .blockingGet();

        Annotation annotation = filteredAnnotations.get(0);
        ParamType type = ParamType.fromAnnotation(annotation);

        return Param.builder()
                .type(type)
                .cls(paramCls)
                .name(type.getName(annotation))
                .required(type.isRequired(annotation))
                .build();
    }

    public static int sort(Param p1, Param p2) {
        return p1.order - p2.order;
    }

    public void apply(Object value) {
        this.value = GSON.toJson(value);
    }

    public static Object instantiate(Param param, Type type) {
        return GSON.fromJson(param.value, type);
    }

    public boolean isInstanceOf(Class<?> itfc) {
        return !Observable.fromArray(this.cls.getInterfaces())
                .filter(aClass -> aClass.equals(itfc))
                .isEmpty().blockingGet();
    }
}
