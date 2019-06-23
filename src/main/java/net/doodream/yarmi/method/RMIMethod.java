package net.doodream.yarmi.method;

import net.doodream.yarmi.annotation.RMIExpose;
import net.doodream.yarmi.annotation.method.Delete;
import net.doodream.yarmi.annotation.method.Get;
import net.doodream.yarmi.annotation.method.Post;
import net.doodream.yarmi.annotation.method.Put;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

/**
 * Created by innocentevil on 18. 5. 4.
 */

public enum RMIMethod {
    GET(Get.class),
    POST(Post.class),
    PUT(Put.class),
    DELETE(Delete.class),
    EXPOSED_METHOD(RMIExpose.class);

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
        } else if (EXPOSED_METHOD.isTypeOf(methodAnnotation)) {
            return EXPOSED_METHOD;
        }
        throw new IllegalArgumentException("No Matched RMIMethod");
    }

    private boolean isTypeOf(Annotation annotation) {
        switch (this) {
            case DELETE:
            case GET:
            case POST:
            case PUT:
            case EXPOSED_METHOD:
                return methodCls.getName().equalsIgnoreCase(annotation.annotationType().getName());
        }
        return false;
    }

    public String extractPath(java.lang.reflect.Method method) {
        //  want to make like -> return method.getAnnotation(this.methodCls).value();
        //  but still inheritance of annotation is not supported (as I know)
        // TODO : fix if Java provides annotation inheritance feature


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
            case EXPOSED_METHOD:
                return "";
            default:
                throw new IllegalArgumentException("Invalid method");
        }
    }

    public static boolean isValidMethod(Method method) {
        return (method.getAnnotation(Get.class) != null) ||
                (method.getAnnotation(Post.class) != null) ||
                (method.getAnnotation(Put.class) != null) ||
                (method.getAnnotation(RMIExpose.class) != null) ||
                (method.getAnnotation(Delete.class) != null);
    }
}
