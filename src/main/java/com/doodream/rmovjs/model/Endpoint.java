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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

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

    RMIMethod method;
    String path;
    List<Param> params;
    String unique;
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

    private static List<Param> convertParams(Endpoint endpoint, Object[] objects) {
        if(objects == null) {
            return Collections.EMPTY_LIST;
        }

        return Observable.fromIterable(endpoint.params).zipWith(Observable.fromArray(objects), (param, o) -> {
            param.apply(o);
            if(param.isInstanceOf(BlobSession.class)) {
                Log.debug("Endpoint {} has session param : {}", endpoint, param);
                if(o != null) {
                    endpoint.session = (BlobSession) o;
                }
            }
            return param;
        }).collectInto(new ArrayList<Param>(), List::add).blockingGet();
    }

    public Request toRequest(Object ...args) {
        if(args == null) {
            return Request.builder()
                    .params(Collections.EMPTY_LIST)
                    .endpoint(getUnique())
                    .build();
        } else {
            return Request.builder()
                    .params(convertParams(this, args))
                    .endpoint(getUnique())
                    .session(session)
                    .build();
        }
    }
}
