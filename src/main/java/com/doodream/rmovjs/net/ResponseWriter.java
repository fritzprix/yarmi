package com.doodream.rmovjs.net;

import com.doodream.rmovjs.model.Response;

import java.io.IOException;

public interface ResponseWriter {
    void write(Response response) throws IOException;
}
