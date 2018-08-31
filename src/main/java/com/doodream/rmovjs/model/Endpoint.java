package com.doodream.rmovjs.model;


import com.doodream.rmovjs.annotation.method.Delete;
import com.doodream.rmovjs.annotation.method.Get;
import com.doodream.rmovjs.annotation.method.Post;
import com.doodream.rmovjs.annotation.method.Put;
import com.doodream.rmovjs.annotation.server.Controller;
import com.doodream.rmovjs.method.RMIMethod;
import com.doodream.rmovjs.net.session.BlobSession;
import com.doodream.rmovjs.parameter.Param;
import com.doodream.rmovjs.util.Types;
import com.google.common.base.Preconditions;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
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
    transient Type unwrappedRetType;
    transient BlobSession session;

    public static Endpoint create(Controller controller, Method method) {

        Preconditions.checkArgument(method.getReturnType().equals(Response.class));

        Annotation methodAnnotation = Observable
                .fromArray(method.getAnnotations())
                .filter(new Predicate<Annotation>() {
                    @Override
                    public boolean test(Annotation annotation) throws Exception {
                        return Endpoint.verifyMethod(annotation);
                    }
                })
                .blockingFirst();

        final String parentPath = controller.path();

        RMIMethod rmiMethod = RMIMethod.fromAnnotation(methodAnnotation);
        Observable<Type> typeObservable = Observable.fromArray(method.getGenericParameterTypes());

        Observable<Annotation[]> annotationsObservable = Observable.fromArray(method.getParameterAnnotations());

        final String paramUnique = typeObservable
                .defaultIfEmpty(Void.class)
                .map(new Function<Type, String>() {
                    @Override
                    public String apply(Type type) throws Exception {
                        return type.getTypeName();
                    }
                })
                .map(new Function<String, String>() {
                    @Override
                    public String apply(String s) throws Exception {
                        return "_".concat(s);
                    }
                })
                .reduce(new BiFunction<String, String, String>() {
                    @Override
                    public String apply(String s, String s2) throws Exception {
                        return s.concat(s2);
                    }
                })
                .blockingGet();

        Single<Long> respBlobObservable = Observable.fromArray(method.getGenericReturnType().getTypeName())
                .map(new Function<String, Matcher>() {
                    @Override
                    public Matcher apply(String s) throws Exception {
                        return TYPE_PATTERN.matcher(s);
                    }
                })
                .filter(new Predicate<Matcher>() {
                    @Override
                    public boolean test(Matcher matcher) throws Exception {
                        return matcher.matches();
                    }
                })
                .map(new Function<Matcher, String>() {
                    @Override
                    public String apply(Matcher matcher) throws Exception {
                        return matcher.group(1);
                    }
                })
                .filter(new Predicate<String>() {
                    @Override
                    public boolean test(String s) throws Exception {
                        return s.contains(BlobSession.class.getName());
                    }
                })
                .count();

        final Long blobCount = typeObservable
                .filter(new Predicate<Type>() {
                    @Override
                    public boolean test(Type type) throws Exception {
                        return type.equals(BlobSession.class);
                    }
                })
                .count().zipWith(respBlobObservable, new BiFunction<Long, Long, Long>() {
                    @Override
                    public Long apply(Long aLong, Long aLong2) throws Exception {
                        return Math.addExact(aLong, aLong2);
                    }
                }).blockingGet();

        if(blobCount > 1) {
            throw new IllegalArgumentException(String.format("too many BlobSession in method @ %s", method.getName()));
        }


        final String path = String.format(Locale.ENGLISH, "%s%s", parentPath, rmiMethod.extractPath(method)).replaceAll(DUPLICATE_PATH_SEPARATOR, "/");

        final AtomicInteger order = new AtomicInteger(0);

        List<Param> params = typeObservable
                .zipWith(annotationsObservable, new BiFunction<Type, Annotation[], Param>() {
                    @Override
                    public Param apply(Type type, Annotation[] annotations) throws Exception {
                        return Param.create(type, annotations);
                    }
                })
                .doOnNext(new Consumer<Param>() {
                    @Override
                    public void accept(Param param) throws Exception {
                        param.setOrder(order.getAndIncrement());
                    }
                })
                .toList().blockingGet();

        final String methodLookupKey = String.format("%x%x%x", rmiMethod.name().hashCode(), controller.path().hashCode(), paramUnique.hashCode()).toUpperCase();
        Type retType;
        try {
            retType = Types.unwrapType(method.getGenericReturnType().getTypeName())[0];
        } catch (ClassNotFoundException | IllegalArgumentException e) {
            retType = method.getGenericReturnType();
        }

        return Endpoint.builder()
                .method(rmiMethod)
                .params(params)
                .jMethod(method)
                .unwrappedRetType(retType)
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
