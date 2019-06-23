package net.doodream.yarmi.serde.bson;

import com.doodream.cutils.Types;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.undercouch.bson4jackson.BsonFactory;
import de.undercouch.bson4jackson.BsonGenerator;
import de.undercouch.bson4jackson.BsonParser;
import net.doodream.yarmi.serde.Converter;
import net.doodream.yarmi.serde.Reader;
import net.doodream.yarmi.serde.Writer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;

public class BsonConverter implements Converter {
    private static final Logger Log = LoggerFactory.getLogger(BsonConverter.class);

    private ObjectMapper objectMapper;
    private BsonFactory bsonFactory;
    private final ExecutorService executorService = Executors.newWorkStealingPool();

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
    protected void finalize() throws Throwable {
        super.finalize();
        if(executorService.isShutdown() || executorService.isTerminated()) {
            return;
        }
        executorService.shutdown();
    }

    @Override
    public Reader reader(final InputStream inputStream) {
        try {
            return new Reader() {

                private final BsonParser parser = bsonFactory.createParser(new BufferedInputStream(inputStream));

                @Override
                public synchronized <T> T read(Class<T> cls) throws IOException {
                    return parser.readValueAs(cls);
                }

                @Override
                public synchronized <T> T read(Class<T> cls, long timeout, TimeUnit timeUnit) throws IOException, TimeoutException {
                    Future<T> result = executorService.submit(() -> {
                        return parser.readValueAs(cls);
                    });
                    try {
                        return result.get(timeout, timeUnit);
                    } catch (InterruptedException e) {
                        throw new TimeoutException(e.getMessage());
                    } catch (ExecutionException e) {
                        throw new IOException(e);
                    }
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

                private final BsonGenerator bsonGenerator = bsonFactory.createGenerator(outputStream);

                @Override
                public synchronized void write(Object src) throws IOException {
                    bsonGenerator.writeObject(src);
                }

                // => max due time is managed by client policy, instead of I/O configuration
                @Override
                public synchronized void write(Object src, long timeout, TimeUnit unit) throws TimeoutException {
                    final Future<Boolean> writeTask = executorService.submit(() -> {
                        try {
                            bsonGenerator.writeObject(src);
                            return true;
                        } catch (IOException e) {
                            return false;
                        }
                    });
                    try {
                        writeTask.get(timeout, unit);
                    } catch (InterruptedException | ExecutionException e) {
                        throw new TimeoutException(e.getMessage());
                    }
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
    public Object resolve(final Object unresolved, Type type) throws InstantiationException, IllegalAccessException {
        if(unresolved == null) {
            return null;
        }
        if(type.equals(Class.class) && (unresolved instanceof String)) {
            try {
                return Class.forName((String) unresolved);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
        Class clsz;
        try {
            if (type instanceof ParameterizedType) {
                clsz = Class.forName(Types.getTypeName(((ParameterizedType) type).getRawType()));
            } else {
                clsz = Class.forName(Types.getTypeName(type));
            }
        } catch (ClassNotFoundException e) {
            return unresolved;
        }
        final Class cls = clsz;
        final Class unresolvedCls = unresolved.getClass();

        if(cls.isInterface()) {
            Type[] typeArguments = ((ParameterizedType) type).getActualTypeArguments();
            if (typeArguments == null || (typeArguments.length == 0)) {
                return unresolved;
            }

            if (cls.equals(List.class)) {
                ArrayList unresolvedList = (ArrayList) unresolved;
                final ArrayList<Object> resolvedList = new ArrayList<>();
                for (Object o : unresolvedList) {
                    resolvedList.add(resolve(o, typeArguments[0]));
                }
                return resolvedList;
            } else if (cls.equals(Map.class)) {
                HashMap<?, Object> unresolvedMap = (HashMap) unresolved;
                for (Map.Entry<?, Object> entry : unresolvedMap.entrySet()) {
                    entry.setValue(resolve(entry.getValue(), typeArguments[1]));
                }
                return unresolvedMap;
            } else if (cls.equals(Set.class)) {
                HashSet<Object> unresolvedSet = new HashSet<>((List<Object>)unresolved);
                final Set<Object> resolvedSet = new HashSet<>();
                for (Object o : unresolvedSet) {
                    resolvedSet.add(resolve(o, typeArguments[0]));
                }
                return resolvedSet;
            } else {
                Log.warn("fail to handle {} {}", unresolved, cls);
                return unresolved;
            }
        } else {
            if(unresolvedCls.equals(LinkedHashMap.class)) {
                return resolveKvMap((Map<?, ?>) unresolved, cls);
            }
        }

        if(cls.equals(unresolvedCls) ||
                Types.isCastable(unresolved, type) ||
                Types.isCastable(unresolved, cls)) {
            return cls.cast(unresolved);
        }



        if(unresolvedCls.getSuperclass().equals(Number.class)) {
            // TODO: 18. 12. 5 handle subclass type of Number
        }

        try {
            Constructor<?> constructor = cls.getConstructor(unresolvedCls);
            return constructor.newInstance(unresolved);
        } catch (NoSuchMethodException | InvocationTargetException ignored) {

        }
        try {
            Method valueOf = cls.getMethod("valueOf", String.class);
            return valueOf.invoke(null, String.valueOf(unresolved));
        } catch (NoSuchMethodException | InvocationTargetException ignored) {

        }
        return unresolved;

    }

    private Object resolveKvMap(final Map<?, ?> map, Class cls) throws IllegalAccessException, InstantiationException {
        final Object object = cls.newInstance();
        final Field[] fields = cls.getDeclaredFields();
        for (Field field : fields) {
            if(map.containsKey(field.getName())) {
                final Type fieldType = field.getGenericType();
                Object resolvedField = resolve(map.get(field.getName()), fieldType);
                field.setAccessible(true);
                field.set(object, resolvedField);
            }
        }

        return object;
    }

}
