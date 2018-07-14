package com.doodream.rmovjs.serde;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * considering concurrent RMI call scenario, reader operation (like read) should be atomic
 *
 */
public interface Reader {
    <T> T read(Class<T> cls) throws IOException;
    <T> T read(Class<T> rawClass, Class<?> parameter) throws IOException;
}
