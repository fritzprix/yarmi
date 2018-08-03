package com.doodream.rmovjs.serde;

import java.io.IOException;

/**
 * considering concurrent RMI call scenario, reader operation (like read) should be atomic
 *
 */
public interface Reader {
    <T> T read(Class<T> cls) throws IOException;
}
