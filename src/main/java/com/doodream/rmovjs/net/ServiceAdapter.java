package com.doodream.rmovjs.net;


import com.doodream.rmovjs.model.Request;
import com.doodream.rmovjs.model.Response;
import com.doodream.rmovjs.model.ServiceInfo;
import io.reactivex.functions.Function;

import java.io.IOException;

public interface ServiceAdapter {
    void listen(ServiceInfo serviceInfo, Function<Request, Response> requestHandler) throws IOException;
    void close() throws IOException;
}
