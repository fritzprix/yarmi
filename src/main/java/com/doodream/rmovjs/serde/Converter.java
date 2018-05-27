package com.doodream.rmovjs.serde;

import com.doodream.rmovjs.model.RMIServiceInfo;
import com.doodream.rmovjs.model.Request;
import io.reactivex.Observable;

import java.io.*;

/**
 *  deals with converting object into byte stream and vice-versa
 */
public interface Converter {

    void write(Object src, Writer writer) throws IOException;

    <T> T read(Reader reader, Class<T> rawClass, Class<?> parameter) throws IOException;

    Reader reader(InputStream inputStream) throws UnsupportedEncodingException;

    Writer writer(OutputStream outputStream) throws UnsupportedEncodingException;

    <T> T read(Reader reader, Class<T> cls) throws IOException;

    byte[] convert(Object src) throws UnsupportedEncodingException;

    <T> T invert(byte[] b, Class<T> cls) throws UnsupportedEncodingException;

}
