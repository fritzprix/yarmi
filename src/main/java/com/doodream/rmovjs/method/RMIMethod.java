package com.doodream.rmovjs.method;

import com.doodream.rmovjs.annotation.method.Delete;
import com.doodream.rmovjs.annotation.method.Get;
import com.doodream.rmovjs.annotation.method.Post;
import com.doodream.rmovjs.annotation.method.Put;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

/**
 * Created by innocentevil on 18. 5. 4.
 */

public enum RMIMethod {
    GET(Get.class),
    POST(Post.class),
    PUT(Put.class),
    DELETE(Delete.class);

    private final Class methodCls;

    RMIMethod(Class cls) {
        methodCls = cls;
    }


    public static RMIMethod fromAnnotation(Annotation methodAnnotation) throws IllegalArgumentException {
        if(PUT.isTypeOf(methodAnnotation)) {
           return PUT;
        } else if(POST.isTypeOf(methodAnnotation)) {
            return POST;
        } else if(GET.isTypeOf(methodAnnotation)) {
            return GET;
        } else if (DELETE.isTypeOf(methodAnnotation)) {
            return DELETE;
        }
        throw new IllegalArgumentException("No Matched RMIMethod");
    }

    private boolean isTypeOf(Annotation annotation) {
        switch (this) {
            case DELETE:
                return methodCls.getName().equalsIgnoreCase(annotation.annotationType().getName());
            case GET:
                return methodCls.getName().equalsIgnoreCase(annotation.annotationType().getName());
            case POST:
                return methodCls.getName().equalsIgnoreCase(annotation.annotationType().getName());
            case PUT:
                return methodCls.getName().equalsIgnoreCase(annotation.annotationType().getName());
        }
        return false;
    }

    public String extractPath(java.lang.reflect.Method method) {
        switch (this) {
            case GET:
                Get get = method.getAnnotation(Get.class);
                return get.value();
            case POST:
                Post post = method.getAnnotation(Post.class);
                return post.value();
            case PUT:
                Put put = method.getAnnotation(Put.class);
                return put.value();
            case DELETE:
                Delete delete = method.getAnnotation(Delete.class);
                return delete.value();
            default:
                throw new IllegalArgumentException("Invalid method");
        }
    }

    public static boolean isValidMethod(Method method) {
        return (method.getAnnotation(Get.class) != null) ||
                (method.getAnnotation(Post.class) != null) ||
                (method.getAnnotation(Put.class) != null) ||
                (method.getAnnotation(Delete.class) != null);
    }
}
