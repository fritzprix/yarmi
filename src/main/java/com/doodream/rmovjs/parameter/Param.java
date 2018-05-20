package com.doodream.rmovjs.parameter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.reactivex.Observable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.lang.annotation.Annotation;
import java.util.List;

/**
 * Created by innocentevil on 18. 5. 4.
 */

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class Param {

    private static final Gson GSON = new GsonBuilder().create();

    private int order;
    private ParamType type;
    private boolean required;
    private String name;
    private Class paramCls;
    private String value;

    public static Param create(Class paramCls, Annotation[] annotations) {

        List<Annotation> filteredAnnotations = Observable.fromArray(annotations)
                .filter(ParamType::isSupportedAnnotation)
                .toList().blockingGet();

        Annotation annotation = filteredAnnotations.get(0);
        ParamType type = ParamType.fromAnnotation(annotation);

        return Param.builder()
                .type(type)
                .name(type.getName(annotation))
                .required(type.isRequired(annotation))
                .paramCls(paramCls)
                .build();
    }

    public static <R> Object instantiate(Param param) {
        return GSON.fromJson(param.value, param.paramCls);
    }

    public static int sort(Param param, Param param1) {
        return param.order - param1.order;
    }
}
