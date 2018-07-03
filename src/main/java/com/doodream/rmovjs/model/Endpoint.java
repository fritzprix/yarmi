package com.doodream.rmovjs.model;


import com.doodream.rmovjs.annotation.method.Delete;
import com.doodream.rmovjs.annotation.method.Get;
import com.doodream.rmovjs.annotation.method.Post;
import com.doodream.rmovjs.annotation.method.Put;
import com.doodream.rmovjs.annotation.server.Controller;
import com.doodream.rmovjs.method.RMIMethod;
import com.doodream.rmovjs.net.session.BlobSession;
import com.doodream.rmovjs.parameter.Param;
import com.google.common.base.Preconditions;
import io.reactivex.Observable;
import io.reactivex.Single;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.*;
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
    private static final Logger Log = LoggerFactory.getLogger(Endpoint.class);
    private static final String DUPLICATE_PATH_SEPARATOR = "\\/{2,}";
    private static final Pattern TYPE_PATTERN = Pattern.compile("[^\\<\\>]+\\<([\\s\\S]+)\\>");

    private RMIMethod method;
    private String path;
    private List<Param> params;
    private String unique;
    transient Method jMethod;
    transient BlobSession session;

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
                .map(Type::getTypeName)
                .map("_"::concat)
                .reduce(String::concat)
                .blockingGet();

        Single<Long> respBlobObservable = Observable.fromArray(method.getGenericReturnType().getTypeName())
                .map(TYPE_PATTERN::matcher)
                .filter(Matcher::matches)
                .map(matcher -> matcher.group(1))
                .filter(s -> s.contains(BlobSession.class.getName()))
                .count();

        final Long blobCount = typeObservable
                .filter(aClass -> aClass.equals(BlobSession.class))
                .count().zipWith(respBlobObservable, Math::addExact).blockingGet();

        if(blobCount > 1) {
            throw new IllegalArgumentException(String.format("too many BlobSession in method @ %s", method.getName()));
        }


        final String path = String.format(Locale.ENGLISH, "%s%s", parentPath, rmiMethod.extractPath(method)).replaceAll(DUPLICATE_PATH_SEPARATOR, "/");

        final int[] order = {0};

        List<Param> params = typeObservable
                .zipWith(annotationsObservable, Param::create)
                .doOnNext(param -> param.setOrder(order[0]++))
                .toList().blockingGet();

        final String methodLookupKey = String.format("%x%x%x", rmiMethod.name().hashCode(), controller.path().hashCode(), paramUnique.hashCode()).toUpperCase();

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

}
