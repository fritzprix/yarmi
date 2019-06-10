package net.doodream.yarmi.serde;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 *  object writer used to transfer Object to peer
 *  @NOTE write operation of single object or (with following argument) should be atomic
 */
public interface Writer {
    void write(Object src) throws IOException;
    void write(Object src, long timeout, TimeUnit unit) throws TimeoutException;
}
