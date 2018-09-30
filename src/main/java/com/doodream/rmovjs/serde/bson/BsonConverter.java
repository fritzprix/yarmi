package com.doodream.rmovjs.serde.bson;

import com.doodream.rmovjs.serde.Converter;
import com.doodream.rmovjs.serde.Reader;
import com.doodream.rmovjs.serde.Writer;
import com.doodream.rmovjs.util.Types;
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
import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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
    public Object resolve(final Object unresolved, Type type) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        if(unresolved == null) {
            return null;
        }
        Class clsz;
        if(type instanceof ParameterizedType) {
            clsz = Class.forName(((ParameterizedType) type).getRawType().getTypeName());
        } else {
            clsz = Class.forName(type.getTypeName());
        }
        final Class cls = clsz;
        final Class unresolvedCls = unresolved.getClass();
        if(cls.equals(unresolvedCls) ||
                Types.isCastable(unresolved, type)) {
            return cls.cast(unresolved);
        }

        if(unresolvedCls.equals(LinkedHashMap.class)) {
            return resolveKvMap((Map<?, ?>) unresolved, cls);
        }

        if(unresolvedCls.equals(ArrayList.class)) {
            Type[] typeArguments = ((ParameterizedType) type).getActualTypeArguments();
            if(typeArguments == null || (typeArguments.length == 0)) {
                return unresolved;
            }
            ArrayList unresolvedList = (ArrayList) unresolved;
            return Observable.<ArrayList>fromIterable(unresolvedList).map(new Function() {
                        @Override
                        public Object apply(Object o) throws Exception {
                            return resolve(o, typeArguments[0]);
                        }}).toList().blockingGet();
        }

        try {
            Constructor<?> constructor = cls.getConstructor(unresolvedCls);
            return constructor.newInstance(unresolved);
        } catch (NoSuchMethodException | InvocationTargetException e) {
            Log.error("",e);
            return unresolved;
        }

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
