package net.doodream.yarmi.model;


import com.doodream.cutils.Types;
import net.doodream.yarmi.annotation.method.*;
import net.doodream.yarmi.annotation.server.Controller;
import net.doodream.yarmi.method.RMIMethod;
import net.doodream.yarmi.net.session.BlobSession;
import net.doodream.yarmi.parameter.Param;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by innocentevil on 18. 5. 4.
 */

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

    private Endpoint() { }


    private static class Builder {
        private final Endpoint endpoint = new Endpoint();

        public Builder method(RMIMethod rmiMethod) {
            endpoint.method = rmiMethod;
            return this;
        }

        public Builder params(List<Param> params) {
            endpoint.params = params;
            return this;
        }

        public Builder jMethod(Method method) {
            endpoint.jMethod = method;
            return this;
        }

        public Builder unwrappedRetType(Type retType) {
            endpoint.unwrappedRetType = retType;
            return this;
        }

        public Builder unique(String unique) {
            endpoint.unique = unique;
            return this;
        }

        public Builder path(String path) {
            endpoint.path = path;
            return this;
        }

        public Endpoint build() {
            return endpoint;
        }
    }

    public static Endpoint create(Controller controller, Method method) throws IllegalArgumentException {
        if(!method.getReturnType().equals(Response.class)) {
            throw new IllegalArgumentException("method should return Response<>");
        }


        Annotation methodAnnotation = getRMIAnnotation(method);
        final String parentPath = controller.path();
        final String unique = getUniqueSignature(method);
        RMIMethod rmiMethod = RMIMethod.fromAnnotation(methodAnnotation);

        final int blobCount = calcBlobSessionCount(method);


//        Single<Long> respBlobObservable = Observable.fromArray(Types.getTypeName(method.getGenericReturnType()))
//                .map(s -> TYPE_PATTERN.matcher(s))
//                .filter(matcher -> matcher.matches())
//                .map(matcher -> matcher.group(1))
//                .filter(s -> s.contains(BlobSession.class.getName()))
//                .count();
//
//        final Long blobCount = typeObservable
//                .filter(type -> type.equals(BlobSession.class))
//                .count()
//                .zipWith(respBlobObservable, (aLong, aLong2) -> aLong + aLong2)
//                .blockingGet();

        if(blobCount > 1) {
            throw new IllegalArgumentException(String.format("too many(%d) BlobSession in method @ %s", blobCount, method.getName()));
        }


        final String path = String.format(Locale.ENGLISH, "%s%s", parentPath, rmiMethod.extractPath(method)).replaceAll(DUPLICATE_PATH_SEPARATOR, "/");
        List<Param> params = buildParamList(method);

        final String methodLookupKey = String.format("%x%x%x", method.hashCode(), controller.path().hashCode(), unique.hashCode()).toUpperCase();
        Type retType;
        try {
            retType = Types.unwrapType(method.getGenericReturnType().toString())[0];
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

    private static List<Param> buildParamList(Method method) {
        final Annotation[][] annotations = method.getParameterAnnotations();
        final List<Param> paramList = new ArrayList<>();
        Type[] types = method.getGenericParameterTypes();
        for (int i = 0; i < types.length; i++) {
            final Param param = Param.create(types[i], annotations[i]);
            param.setOrder(i);
            paramList.add(param);
        }
        return paramList;
    }

    private static int calcBlobSessionCount(Method method) {
        int count = 0;
        final Matcher matcher = TYPE_PATTERN.matcher(Types.getTypeName(method.getGenericReturnType()));
        if(matcher.matches()) {
            if(matcher.group(1).contains(BlobSession.class.getName())) {
                count++;
            }
        }
        Type[] types = method.getGenericParameterTypes();
        for (Type type : types) {
            if(type.equals(BlobSession.class)) {
                count++;
            }
        }
        return count;
    }

    private static String getUniqueSignature(Method method) {
        final List<Type> types = Arrays.asList(method.getGenericParameterTypes());
        if(types.isEmpty()) {
            types.add(Void.class);
        }
        StringBuilder builder = new StringBuilder();
        for (Type type : types) {
            builder.append(type.getTypeName().concat("_"));
        }
        return builder.toString();
    }

    private static Annotation getRMIAnnotation(Method method) {
        final Annotation[] annotations = method.getAnnotations();
        for (Annotation annotation : annotations) {
            if(verifyMethod(annotation)) {
                return annotation;
            }
        }

        return null;
    }

    private static Builder builder() {
        return new Builder();
    }

    public String getUnique() {
        return unique;
    }

    public List<Param> getParams() {
        return params;
    }

    public Method getJMethod() {
        return jMethod;
    }

    public BlobSession getSession() {
        return session;
    }

    public Type getUnwrappedRetType() {
        return unwrappedRetType;
    }

    public void setMethod(RMIMethod method) {
        this.method = method;
    }


    public void setParams(List<Param> params) {
        this.params = params;
    }

    public void setSession(BlobSession session) {
        this.session = session;
    }


    private static boolean verifyMethod(Annotation annotation) {
        Class cls = annotation.annotationType();
        return (cls == Get.class)
                || (cls == RMIExpose.class)
                || (cls == Post.class)
                || (cls == Put.class)
                || (cls == Delete.class);
    }
}
