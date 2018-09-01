package com.doodream.rmovjs.serde.bson;

import com.doodream.rmovjs.serde.Converter;
import com.doodream.rmovjs.serde.Reader;
import com.doodream.rmovjs.serde.Writer;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import de.undercouch.bson4jackson.BsonFactory;
import de.undercouch.bson4jackson.BsonGenerator;
import de.undercouch.bson4jackson.BsonParser;
import io.reactivex.Observable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

public class BsonConverter implements Converter {
    private static final Logger Log = LoggerFactory.getLogger(BsonConverter.class);

    private ObjectMapper objectMapper;
    private BsonFactory bsonFactory;
    public BsonConverter() {

        bsonFactory = new BsonFactory();
        bsonFactory.enable(BsonGenerator.Feature.ENABLE_STREAMING);

        objectMapper = new ObjectMapper(bsonFactory
                .disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET)
                .enable(JsonGenerator.Feature.AUTO_CLOSE_JSON_CONTENT))
                .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                .disable(MapperFeature.AUTO_DETECT_IS_GETTERS, MapperFeature.AUTO_DETECT_GETTERS)
                .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
    }
    @Override
    public Reader reader(final InputStream inputStream) {
        try {
            return new Reader() {

                private BsonParser parser = bsonFactory.createParser(new BufferedInputStream(inputStream));

                @Override
                public synchronized <T> T read(Class<T> cls) throws IOException {
                    return parser.readValueAs(cls);
                }
            };
        } catch (IOException e) {
            Log.error(e.getLocalizedMessage());
            return null;
        }
    }

    @Override
    public Writer writer(final OutputStream outputStream) {
        try {
            return new Writer() {
                private BsonGenerator bsonGenerator = bsonFactory.createGenerator(outputStream);

                @Override
                public synchronized void write(Object src) throws IOException {
                    bsonGenerator.writeObject(src);
                }
            };
        } catch (IOException e) {
            Log.error(e.getLocalizedMessage());
            return null;
        }
    }

    @Override
    public byte[] convert(Object src) {
        try {
            return objectMapper.writeValueAsBytes(src);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return new byte[0];
    }

    @Override
    public <T> T invert(byte[] b, Class<T> cls) {
        try {
            return objectMapper.readValue(b, cls);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


    @Override
    public <T> T resolve(final Object unresolved, Type type) throws ClassNotFoundException {
        if(unresolved == null) {
            return null;
        }
        Log.debug("unresolved : {}", unresolved.getClass());
        Class clsz;
        if(type instanceof ParameterizedType) {
            clsz = Class.forName(((ParameterizedType) type).getRawType().getTypeName());
        } else {
            clsz = Class.forName(type.getTypeName());
        }
        final Class cls = clsz;
        final Class unresolvedCls = unresolved.getClass();
//        if(!type.getTypeName().contains("SCM")) {
//            Log.debug("cls of unresolved : {} / given type : {}", unresolvedCls, cls);
//        }
        if(cls.equals(unresolvedCls)) {
            return (T) unresolved;
        }

        Observable<Object> constructorObservable = Observable.fromArray(cls.getConstructors())
                .filter(new Predicate<Constructor>() {
                    @Override
                    public boolean test(Constructor constructor) throws Exception {
                        return constructor.getParameterCount() == 1;
                    }
                })
                .filter(new Predicate<Constructor>() {
                    @Override
                    public boolean test(Constructor constructor) throws Exception {
                        return constructor.getParameterTypes()[0].equals(unresolvedCls);
                    }
                })
                .map(new Function<Constructor, Object>() {
                    @Override
                    public Object apply(Constructor constructor) throws Exception {
                        return constructor.newInstance(unresolved);
                    }
                });

        Observable<ParameterizedType> parameterizedTypeObservable = Observable.fromArray(unresolvedCls.getGenericInterfaces())
                .cast(ParameterizedType.class)
                .cache();

        Observable<Object> mapObservable = parameterizedTypeObservable
                .filter(new Predicate<ParameterizedType>() {
                    @Override
                    public boolean test(ParameterizedType parameterizedType) throws Exception {
                        return parameterizedType.getRawType().equals(Map.class);
                    }
                })
                .map(new Function<ParameterizedType, Map<?,?>>() {
                    @Override
                    public Map<?, ?> apply(ParameterizedType parameterizedType) throws Exception {
                        return (Map<?,?>) unresolved;
                    }
                })
                .map(new Function<Map<?,?>, Object>() {
                    @Override
                    public Object apply(Map<?, ?> map) throws Exception {
                        return resolveKvMap(map, cls);
                    }
                });

        // TODO: 18. 8. 31 handle collection types other than map
        Observable<List<?>> listObservable = parameterizedTypeObservable
                .filter(new Predicate<ParameterizedType>() {
                    @Override
                    public boolean test(ParameterizedType parameterizedType) throws Exception {
                        return parameterizedType.getRawType().equals(List.class);
                    }
                })
                .map(new Function<ParameterizedType, List<?>>() {
                    @Override
                    public List<?> apply(ParameterizedType parameterizedType) throws Exception {
                        return (List<?>) unresolved;
                    }
                });


        return (T) mapObservable.mergeWith(constructorObservable)
                .mergeWith(listObservable)
                .blockingSingle(unresolved);

    }

    private Object resolveKvMap(final Map<?, ?> map, Class cls) throws IllegalAccessException, InstantiationException {
        final Object resolved = cls.newInstance();
        Observable.fromArray(cls.getDeclaredFields())
                .filter(new Predicate<Field>() {
                    @Override
                    public boolean test(Field field) throws Exception {
                        return map.containsKey(field.getName());
                    }
                })
                .doOnNext(new Consumer<Field>() {
                    @Override
                    public void accept(Field field) throws Exception {
                        field.setAccessible(true);
                        field.set(resolved, map.get(field.getName()));
                    }
                })
                .blockingSubscribe();

        return resolved;
    }

    private <T> T handlePrimitive(Object unresolved, Class cls) {
        return (T) cls.cast(unresolved);
    }
}
