package com.doodream.rmovjs.model;


import com.doodream.rmovjs.annotation.method.Delete;
import com.doodream.rmovjs.annotation.method.Get;
import com.doodream.rmovjs.annotation.method.Post;
import com.doodream.rmovjs.annotation.method.Put;
import com.doodream.rmovjs.annotation.server.Controller;
import com.doodream.rmovjs.method.RMIMethod;
import com.doodream.rmovjs.parameter.Param;
import com.doodream.rmovjs.util.SerdeUtil;
import com.google.common.base.Preconditions;
import io.reactivex.Observable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Created by innocentevil on 18. 5. 4.
 */

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class Endpoint {
    private static final Logger Log = LogManager.getLogger(Endpoint.class);
    private static final String DUPLICATE_PATH_SEPARATOR = "\\/{2,}";
    private static final Pattern TYPE_PATTNER = Pattern.compile("\\<([\\s\\S]+)\\>");

    RMIMethod method;
    String path;
    List<Param> params;
    String unique;
    transient Method jMethod;

    public static Endpoint create(Controller controller, Method method) {

        Preconditions.checkArgument(method.getReturnType().equals(Response.class));

        Annotation methodAnnotation = Observable
                .fromArray(method.getAnnotations())
                .filter(Endpoint::verifyMethod)
                .blockingFirst();

        final String parentPath = controller.path();

        RMIMethod rmiMethod = RMIMethod.fromAnnotation(methodAnnotation);
        Observable<Class> typeObservable = Observable.fromArray(method.getParameterTypes());
        Observable<Annotation[]> annotationsObservable = Observable.fromArray(method.getParameterAnnotations());

        final String paramUnique = typeObservable
                .defaultIfEmpty(Void.class)
                .map(Class::getName)
                .map("_"::concat)
                .reduce(String::concat)
                .blockingGet();

        final String path = String.format(Locale.ENGLISH, "%s%s", parentPath, rmiMethod.extractPath(method)).replaceAll(DUPLICATE_PATH_SEPARATOR, "/");

        final int[] order = {0};
        List<Param> params = typeObservable
                .zipWith(annotationsObservable, Param::create)
                .doOnNext(param -> param.setOrder(order[0]++))
                .toList().blockingGet();


        final String methodLookupKey = rmiMethod.name().concat(path.concat(paramUnique));

        return Endpoint.builder()
                .method(rmiMethod)
                .params(params)
                .jMethod(method)
                .unique(methodLookupKey)
                .path(path)
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
            param.setValue(SerdeUtil.toJson(o));
            return param;
        }).subscribe();
    }
}
