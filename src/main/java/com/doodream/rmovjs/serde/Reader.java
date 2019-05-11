package com.doodream.rmovjs.serde;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * considering concurrent RMI call scenario, reader operation (like read) should be atomic
 *
 */
public interface Reader {
    <T> T read(Class<T> cls) throws IOException;
    <T> T read(Class<T> cls, long timeout, TimeUnit timeUnit) throws IOException, TimeoutException;
}
