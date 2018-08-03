package com.doodream.rmovjs.serde.json;

import com.doodream.rmovjs.serde.Converter;
import com.doodream.rmovjs.serde.Reader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class JsonReader implements Reader {
    private BufferedReader mBufferedReader;
    private Converter mConverter;

    JsonReader(Converter converter, InputStream is) {
        mConverter = converter;
        mBufferedReader = new BufferedReader(new InputStreamReader(is));
    }


    @Override
    public synchronized  <T> T read(Class<T> cls) throws IOException {
        String line = mBufferedReader.readLine();
        if(line == null) {
            return null;
        }
        line = line.trim();
        return mConverter.invert(StandardCharsets.UTF_8.encode(line).array(), cls);
    }

//    @Override
//    public synchronized  <T> T read(Class<T> rawClass, Class<?> parameter) throws IOException {
//        String line = mBufferedReader.readLine();
//        if(line == null) {
//            return null;
//        }
//        line = line.trim();
//        return mConverter.invert(StandardCharsets.UTF_8.encode(line).array(), rawClass, parameter);
//    }
}
