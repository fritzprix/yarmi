package com.doodream.rmovjs.model;


import com.doodream.rmovjs.annotation.method.Delete;
import com.doodream.rmovjs.annotation.method.Get;
import com.doodream.rmovjs.annotation.method.Post;
import com.doodream.rmovjs.annotation.method.Put;
import com.doodream.rmovjs.annotation.server.Controller;
import com.doodream.rmovjs.method.RMIMethod;
import com.doodream.rmovjs.parameter.Param;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.reactivex.Observable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by innocentevil on 18. 5. 4.
 */

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class Endpoint {
    private static final String DUPLICATE_PATH_SEPARATOR = "\\/{2,}";
    private static final Pattern TYPE_PATTNER = Pattern.compile("\\<([\\s\\S]+)\\>");
    private static final Gson GSON = new GsonBuilder().create();

    RMIMethod method;
    String path;
    List<Param> params;
    Class responseType;
    transient Method jMethod;

    public static Endpoint create(Controller controller, Method method) {

        assert method.getReturnType().equals(Response.class);

        Annotation methodAnnotation = Observable.fromArray(method.getAnnotations())
                .filter(Endpoint::verifyMethod)
                .blockingFirst();

        final String parentPath = controller.path();

        RMIMethod httpMethod = RMIMethod.fromAnnotation(methodAnnotation);
        Observable<Class> typeObservable = Observable.fromArray(method.getParameterTypes());
        Observable<Annotation[]> annotationsObservable = Observable.fromArray(method.getParameterAnnotations());

        final int[] order = {0};
        List<Param> params = typeObservable
                .zipWith(annotationsObservable, Param::create)
                .doOnNext(param -> param.setOrder(order[0]++))
                .toList().blockingGet();

        Observable<Class> responseClassObservable = Observable.just(TYPE_PATTNER.matcher(method.getGenericReturnType().getTypeName()))
                .filter(Matcher::find)
                .map(matcher -> matcher.group(1))
                .map(Class::forName);

        return Endpoint.builder()
                .method(httpMethod)
                .params(params)
                .jMethod(method)
                .responseType(responseClassObservable.blockingFirst())
                .path(String.format("%s%s",parentPath, httpMethod.extractPath(method)).replaceAll(DUPLICATE_PATH_SEPARATOR,"/"))
                .build();
    }

    private static boolean verifyMethod(Annotation annotation) {
        Class cls = annotation.annotationType();
        return (cls == Get.class)
                || (cls == Post.class)
                || (cls == Put.class)
                || (cls == Delete.class);
    }

    public void applyParam(Object[] objects) {
        if(objects == null) {
            return;
        }

        Observable.fromIterable(params).zipWith(Observable.fromArray(objects), (param, o) -> {
            param.setValue(GSON.toJson(o));
            return param;
        }).subscribe();
    }
}
