package com.doodream.rmovjs.serde.bson;

import com.doodream.rmovjs.serde.Converter;
import com.doodream.rmovjs.serde.Reader;
import com.doodream.rmovjs.serde.Writer;
import org.bson.BsonBinary;
import org.bson.BsonDocument;
import org.bson.BsonValue;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

public class BsonConverter implements Converter {
    @Override
    public Reader reader(InputStream inputStream) {
        return null;
    }

    @Override
    public Writer writer(OutputStream outputStream) {
        return null;
    }

    @Override
    public byte[] convert(Object src) throws UnsupportedEncodingException {
        return new byte[0];
    }

    @Override
    public <T> T invert(byte[] b, Class<T> cls) throws UnsupportedEncodingException {
        return null;
    }

    @Override
    public <T> T invert(byte[] b, Class<T> rawClass, Class<?> parameter) {
        return null;
    }
}
