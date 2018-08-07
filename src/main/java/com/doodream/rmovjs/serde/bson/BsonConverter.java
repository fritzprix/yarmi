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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
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
    public Reader reader(InputStream inputStream) {
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
    public Writer writer(OutputStream outputStream) {
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
    public <T> T resolve(Object unresolved, Type type) throws ClassNotFoundException {
        if(unresolved == null) {
            return null;
        }
        Class cls = Class.forName(type.getTypeName());
        Class unresolvedCls = unresolved.getClass();
//        if(!type.getTypeName().contains("SCM")) {
//            Log.debug("cls of unresolved : {} / given type : {}", unresolvedCls, cls);
//        }
        if(cls.equals(unresolvedCls)) {
            return (T) unresolved;
        }

        Observable<Object> constructorObservable = Observable.fromArray(cls.getConstructors())
                .filter(constructor -> constructor.getParameterCount() == 1)
                .filter(constructor -> constructor.getParameterTypes()[0].equals(unresolvedCls))
                .map(constructor -> constructor.newInstance(unresolved));

        Observable<ParameterizedType> parameterizedTypeObservable = Observable.fromArray(unresolvedCls.getGenericInterfaces())
                .cast(ParameterizedType.class)
                .cache();

        Observable<Object> mapObservable = parameterizedTypeObservable
                .filter(typeParam -> typeParam.getRawType().equals(Map.class))
                .map(typeParam -> (Map<?,?>) unresolved)
                .map(map -> resolveKvMap(map, cls));

        return (T) mapObservable.mergeWith(constructorObservable)
                .blockingSingle(unresolved);

    }

    private Object resolveKvMap(Map<?, ?> map, Class cls) throws IllegalAccessException, InstantiationException {
        Object resolved = cls.newInstance();
        Observable.fromArray(cls.getDeclaredFields())
                .filter(field -> map.containsKey(field.getName()))
                .doOnNext(field -> field.setAccessible(true))
                .doOnNext(field -> field.set(resolved, map.get(field.getName())))
                .blockingSubscribe();

        return resolved;
    }

    private <T> T handlePrimitive(Object unresolved, Class cls) {
        return (T) cls.cast(unresolved);
    }
}
