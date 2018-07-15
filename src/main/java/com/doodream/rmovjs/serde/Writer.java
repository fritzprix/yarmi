package com.doodream.rmovjs.serde;

import java.io.IOException;

/**
 *  object writer used to transfer Object to peer
 *  @NOTE write operation of single object or (with following argument) should be atomic
 */
public interface Writer {
    void write(Object src) throws IOException;
}
